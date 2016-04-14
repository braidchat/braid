(ns braid.ui.views.new-message
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [clojure.string :as string]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.emoji :as emoji]
            [chat.client.views.helpers :refer [id->color debounce]]
            [clj-fuzzy.metrics :as fuzzy])
  (:import [goog.events KeyCodes]))

(defn- normalize [s]
  (-> (.toLowerCase s)
     (string/replace #"\s" "")))

(defn- simple-matches?
  [m s]
  (not= -1 (.indexOf m s)))

(defn- fuzzy-matches? [m s]
  (let [m (normalize m)
        s (normalize s)]
    (or (simple-matches? m s)
        (< (fuzzy/levenshtein m s) 2))))

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


(defn emoji-view
  [emoji]
  [:div.emoji-match
    (emoji/shortcode->html (string/replace emoji #"[\(\)]" ":"))
    [:div.name emoji]
    [:div.extra "..."]])

(def engines
  [
   ; ... :emoji  -> autocomplete emoji
   (fn [text thread-id]
     (let [pattern #"\B[:(](\S{2,})$"]
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
                        [emoji-view (let [show-brackets? (= "(" (first text))
                                          emoji-name (apply str (-> k rest butlast))]
                                      (if show-brackets?
                                         (str "(" emoji-name ")") k))
                                  {:react-key k}])}))))))

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
                        [:div.user-match
                          [:img.avatar {:src (user :avatar)}]
                          [:div.name (user :nickname)]
                          [:div.extra "..."]])}))))))

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
                        [:div.tag-match
                          [:div.color-block{:style {:backgroundColor (id->color (tag :id))}}]
                          [:div.name (tag :name)]
                          [:div.extra (:name (store/id->group (tag :group-id)))]])}))))))
   ])

(defn- auto-resize [el]
  (set! (.. el -style -height) "auto")
  (set! (.. el -style -height)
        (str (min 300 (.-scrollHeight el)) "px")))

(defn new-message-view [config owner]
  (let [init-state (r/atom {:text ""
                            :force-close? false
                            :highlighted-result-index 0
                            :results nil
                            :kill-chan (chan)
                            :autocomplete-chan (chan)})

        get-state (fn [k] (@init-state k))

        set-state! (fn [k v] (swap! init-state assoc k v))

        update-state! (fn
                        ([f] (swap! init-state f))
                        ([korks f]
                           (if (keyword? korks)
                             (swap! init-state update-in [korks] f)
                             (swap! init-state update-in korks f))))
        get-node (fn [a] )]
    (r/create-class
      {:component-will-mount
       (fn []
         (let [autocomplete (debounce (get-state :autocomplete-chan) 100)
               kill-chan (get-state :kill-chan)]
           (go (loop []
                 (let [[v ch] (alts! [autocomplete kill-chan])]
                    (when (= ch autocomplete)
                      (update-state! #(assoc %
                                           :results (seq (mapcat (fn [e] (e v (config :thread-id))) engines))
                                           :highlighted-result-index 0))
                      (recur)))))))
       :component-did-mount
       (fn []
         (when (= (config :thread-id) (store/get-new-thread))
           (store/clear-new-thread!)
           (.focus (get-node "message-text")))
         (let [textarea (get-node "message-text")]
           (auto-resize textarea)
           (.. js/window (addEventListener "resize" (fn [_]
                                                      (auto-resize textarea))))))
       :component-will-unmount
       (fn []
         (put! (get-state :kill-chan) (js/Date.)))
       :component-will-update
       (fn []
         (let [next-text (next-state :text)
               ;TODO: get previous text using reagent not om
               prev-text (om/get-render-state owner :text)]
           (when (not= next-text prev-text)
             (put! (next-state :autocomplete-chan) next-text))))
       :component-did-update
       (fn []
         (auto-resize (get-node "message-text")))
       :reagent-render
       (fn []
         (let [highlight-next!
                (fn []
                  (update-state! :highlighted-result-index
                                    #(mod (inc %) (count results))))
                highlight-prev!
                (fn []
                  (update-state! :highlighted-result-index
                                    #(mod (dec %) (count results))))
                reset-state!
                (fn []
                  (update-state!
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
                  (set-state! :text ((result :message-transform) text)))

                autocomplete-open? (and (not force-close?) (not (nil? results)))

                errors-cursor (->> (om/root-cursor store/app-state) :errors
                                    om/ref-cursor (om/observe owner))

                connected? (not-any? (fn [[k _]] (= :disconnected k))
                                     errors-cursor)]
              [:div.message.new
                [:textarea {:placeholder (config :placeholder)
                            :ref "message-text"
                            :value (state :text)
                            :disabled (not connected?)
                            :on-change (fn [e]
                                        (let [text (.slice (.. e -target -value) 0 5000)]
                                          (update-state!
                                                            (fn [s]
                                                              (assoc s
                                                                :text text
                                                                :force-close? false))))
                                        (auto-resize (.. e -target)))
                            :on-key-down
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
                                        (set-state! :force-close? true))))
                                  ; ENTER otherwise -> send message
                                  (not e.shiftKey)
                                  (do
                                    (.preventDefault e)
                                    (send-message!)))

                                KeyCodes.ESC (do
                                               (set-state! :force-close? true))

                                KeyCodes.UP (when autocomplete-open?
                                              (.preventDefault e)
                                              (highlight-prev!))

                                KeyCodes.DOWN (when autocomplete-open?
                                                (.preventDefault e)
                                                (highlight-next!))
                                nil))}]

                 (when autocomplete-open?
                   [:div.autocomplete
                     (if (seq results)
                       [:div
                         (map-indexed
                           (fn [i result]
                             (:div.result {:className (str " " (when (= i highlighted-result-index)
                                                                 "highlight"))
                                           :style {:cursor "pointer"}
                                           :on-click (fn []
                                                      (choose-result! result)
                                                      (.focus (get-node "message-text")))}
                               ((result :html))))
                           results)]
                       [:div.result
                         "No Results"])])]))})))
