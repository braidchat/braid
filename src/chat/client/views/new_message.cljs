(ns chat.client.views.new-message
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [clojure.string :as string]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.emoji :as emoji]
            [chat.client.views.helpers :refer [id->color debounce]])
  (:import [goog.events KeyCodes]))

(defn fuzzy-matches?
  [s m]
  ; TODO: make this fuzzier? something like interleave with .* & re-match?
  (letfn [(normalize [s]
            (-> (.toLowerCase s) (string/replace #"\s" "")))]
    (not= -1 (.indexOf (normalize s) (normalize m)))))

(defn simple-matches?
  [m s]
  (not= -1 (.indexOf m s)))


; fn that returns results that will be shown if pattern matches
;    inputs:
;       text - current text of user's message
;       thread-id - id of the thread
;    output:
;       if no pattern matched, return nil
;       if a trigger pattern was matched, an array of maps, each containing:
;         :html - fn that returns html to be displayed for the result
;             inputs:
;                 none
;             output:
;                 html (as returned by (dom/*) functions)
;         :action - fn to be triggered when result picked
;             inputs:
;                 thread-id
;             output:
;                 none expected
;         :message-transform - fn to apply to text of message
;             inputs:
;                text
;             output:
;                text to replace message with


(defn emoji-view [emoji owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "emoji-match"}
        (emoji/shortcode->html emoji)
        (dom/div #js {:className "name"}
          emoji)
        (dom/div #js {:className "extra"}
          "...")))))

(def engines
  [
   ; ... :emoji  -> autocomplete emoji
   (fn [text thread-id]
     (let [pattern #"\B:(\S{2,})$"]
       (when-let [query (second (re-find pattern text))]
         (->> emoji/unicode
              (filter (fn [[k v]]
                        (simple-matches? k query)))
              (map (fn [[k v]]
                     {:action
                      (fn [thread-id])
                      :message-transform
                      (fn [text]
                        (string/replace text pattern (str k " ")))
                      :html
                      (fn []
                        (om/build emoji-view k {:react-key k}))}))))))

   ; ... @<user>  -> autocompletes user name
   (fn [text thread-id]
     (let [pattern #"\B@(\S{0,})$"]
       (when-let [query (second (re-find pattern text))]
         (->> (store/users-in-open-group)
              (filter (fn [u]
                        (fuzzy-matches? (u :nickname) query)))
              (map (fn [user]
                     {:action
                      (fn [thread-id])
                      :message-transform
                      (fn [text]
                        (string/replace text pattern (str "@" (user :nickname) " ")))
                      :html
                      (fn []
                        (dom/div #js {:className "user-match"}
                          (dom/img #js {:className "avatar"
                                        :src (user :avatar)})
                          (dom/div #js {:className "name"}
                            (user :nickname))
                          (dom/div #js {:className "extra"}
                            "...")))}))))))

   ; ... #<tag>   -> autocompletes tag
   (fn [text thread-id]
     (let [pattern #"\B#(\S{0,})$"]
       (when-let [query (second (re-find pattern text))]
         (->> (store/tags-in-open-group)
              (filter (fn [t]
                        (fuzzy-matches? (t :name) query)))
              (map (fn [tag]
                     {:action
                      (fn [thread-id])
                      :message-transform
                      (fn [text]
                        (string/replace text pattern (str "#" (tag :name) " ")))
                      :html
                      (fn []
                        (dom/div #js {:className "tag-match"}
                          (dom/div #js {:className "color-block"
                                        :style #js {:backgroundColor (id->color (tag :id))}})
                          (dom/div #js {:className "name"}
                            (tag :name))
                          (dom/div #js {:className "extra"}
                            (:name (store/id->group (tag :group-id))))))}))))))
   ])

(defn- auto-resize [el]
  (set! (.. el -style -height) "auto")
  (set! (.. el -style -height)
        (str (min 300 (.-scrollHeight el)) "px")))

(defn new-message-view [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:text ""
       :force-close? false
       :highlighted-result-index 0
       :results nil
       :kill-chan (chan)
       :autocomplete-chan (chan)
       })

    om/IWillMount
    (will-mount [_]
      (let [autocomplete (debounce (om/get-state owner :autocomplete-chan) 100)
            kill-chan (om/get-state owner :kill-chan)]
        (go (loop []
              (let [[v ch] (alts! [autocomplete kill-chan])]
                (when (= ch autocomplete)
                  (om/update-state! owner
                                    #(assoc %
                                       :results (seq (mapcat (fn [e] (e v (config :thread-id))) engines))
                                       :highlighted-result-index 0))
                  (recur)))))))

    om/IDidMount
    (did-mount [_]
      (when (= (config :thread-id) (store/get-new-thread))
        (store/clear-new-thread!)
        (.focus (om/get-node owner "message-text")))
      (let [textarea (om/get-node owner "message-text")]
        (auto-resize textarea)
        (.. js/window (addEventListener "resize" (fn [_] (auto-resize textarea))))))

    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :kill-chan) (js/Date.)))

    om/IWillUpdate
    (will-update [_ next-props next-state]
      (let [next-text (next-state :text)
            prev-text (om/get-render-state owner :text)]
          (when (not= next-text prev-text)
            (put! (next-state :autocomplete-chan) next-text))))

    om/IDidUpdate
    (did-update [_ _ _]
      (auto-resize (om/get-node owner "message-text")))

    om/IRenderState
    (render-state [_ {:keys [results text force-close? highlighted-result-index] :as state}]
      (let [highlight-next!
            (fn []
              (om/update-state! owner :highlighted-result-index
                                #(mod (inc %) (count results))))
            highlight-prev!
            (fn []
              (om/update-state! owner :highlighted-result-index
                                #(mod (dec %) (count results))))
            reset-state!
            (fn []
              (om/update-state! owner
                             (fn [s]
                               (merge s
                                      {:text ""
                                       :force-close? false
                                       :highlighted-result-index 0}))))
            send-message!
            (fn []
              (store/set-new-thread! (config :thread-id))
              (dispatch! :new-message {:thread-id (config :thread-id)
                                       :content text
                                       :mentioned-user-ids (config :mentioned-user-ids)
                                       :mentioned-tag-ids (config :mentioned-tag-ids)})
              (reset-state!))
            choose-result!
            (fn [result]
              ((result :action) (config :thread-id))
              (om/set-state! owner :text ((result :message-transform) text)))
            autocomplete-open? (and (not force-close?) (not (nil? results)))]
          (dom/div #js {:className "message new"}
            (dom/textarea #js {:placeholder (config :placeholder)
                               :ref "message-text"
                               :value (state :text)
                               :onChange (fn [e]
                                           (let [text (.slice (.. e -target -value) 0 5000)]
                                             (om/update-state! owner
                                                               (fn [s]
                                                                 (assoc s
                                                                   :text text
                                                                   :force-close? false))))
                                           (auto-resize (.. e -target)))
                               :onKeyDown
                               (fn [e]
                                 (condp = e.keyCode
                                   KeyCodes.ENTER
                                   (cond
                                     ; ENTER when autocomplete -> trigger chosen result's action (or exit autocomplete if no result chosen)
                                     autocomplete-open?
                                     (do
                                       (.preventDefault e)
                                       (if-let [result (nth results highlighted-result-index nil)]
                                         (choose-result! result)
                                         (do
                                           (om/set-state! owner :force-close? true))))
                                     ; ENTER otherwise -> send message
                                     (not e.shiftKey)
                                     (do
                                       (.preventDefault e)
                                       (send-message!)))

                                   KeyCodes.ESC (do
                                                  (om/set-state! owner :force-close? true))

                                   KeyCodes.UP (when autocomplete-open?
                                                 (.preventDefault e)
                                                 (highlight-prev!))

                                   KeyCodes.DOWN (when autocomplete-open?
                                                   (.preventDefault e)
                                                   (highlight-next!))
                                   nil))})

            (when autocomplete-open?
              (dom/div #js {:className "autocomplete"}
                (if (seq results)
                  (apply dom/div nil
                    (map-indexed
                      (fn [i result]
                        (dom/div #js {:className (str "result" " "
                                                      (when (= i highlighted-result-index) "highlight"))
                                      :style #js {:cursor "pointer"}
                                      :onClick (fn []
                                                 (choose-result! result)
                                                 (.focus (om/get-node owner "message-text")))}
                          ((result :html))))
                      results))
                  (dom/div #js {:className "result"}
                    "No Results")))))))))
