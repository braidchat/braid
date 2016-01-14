(ns chat.client.views.new-message
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.views.helpers :as helpers]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store])
  (:import [goog.events KeyCodes]))


(defn tee [x]
  (println x) x)

(defn fuzzy-matches?
  [s m]
  (letfn [(normalize [s]
            (-> (.toLowerCase s) (string/replace #"\s" "")))]
    (not= -1 (.indexOf (normalize s) (normalize m)))))


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


(def engines
  [
   ; ... @<user>  -> autocompletes user name
   (fn [text thread-id]
     (let [pattern #"\B@(\S{1,})$"]
       (when-let [query (second (re-find pattern text))]
         (->> (store/all-users)
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
                          (dom/div #js {:className "user-nickname"}
                            (user :nickname))
                          (dom/div #js {:className "group-name"}
                            "...")))}))))))

   ; ... #<tag>   -> autocompletes tag
   (fn [text thread-id]
     (let [pattern #"\B#(\S{1,})$"]
       (when-let [query (second (re-find pattern text))]
         (->> (store/all-tags)
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
                                        :style #js {:backgroundColor (helpers/tag->color tag)}})
                          (dom/div #js {:className "tag-name"}
                            (tag :name))
                          (dom/div #js {:className "group-name"}
                            (:name (store/id->group (tag :group-id))))))}))))))

   ; /<tag-name>  ->  adds tag to message
   (fn [text thread-id]
     (when-let [query (second (re-matches #"^/(\S{1,})" text))]
       (let [thread-tag-ids (-> (store/id->thread thread-id)
                                :tag-ids
                                set)]
         ; suggest tags
         (->> (store/all-tags)
              (remove (fn [t]
                        (contains? thread-tag-ids (t :id))))
              (filter (fn [t]
                        (fuzzy-matches? (t :name) query)))
              (map (fn [tag]
                     {:action
                      (fn [thread-id]
                        (dispatch! :tag-thread {:thread-id thread-id
                                                :id (tag :id)}))
                      :message-transform
                      (fn [text] "")
                      :html
                      (fn []
                        (dom/div #js {:className "tag-match"}
                          (dom/div #js {:className "color-block"
                                        :style #js {:backgroundColor (helpers/tag->color tag)}})
                          (dom/div #js {:className "tag-name"}
                            (tag :name))
                          (dom/div #js {:className "group-name"}
                            (:name (store/id->group (tag :group-id))))))}))))))])


; TODO: autocomplete mentions
(defn new-message-view [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:text ""
       :force-close? false
       :highlighted-result-index -1})
    om/IRenderState
    (render-state [_ {:keys [text force-close? highlighted-result-index] :as state}]
      (let [constrain (fn [x a z]
                        (cond
                          (> x z) z
                          (< x a) a
                          :else x))
            results (let [engine-results (map (fn [e] (e text (config :thread-id))) engines)]
                      (if (every? nil? engine-results)
                         nil
                        (apply concat engine-results)))
            highlight-next!
            (fn []
              (om/update-state! owner :highlighted-result-index
                                #(constrain (inc %) 0 (dec (count results)))))
            highlight-prev!
            (fn []
              (om/update-state! owner :highlighted-result-index
                                #(constrain (dec %) 0 (dec (count results)))))
            highlight-clear!
            (fn []
              (om/set-state! owner :highlighted-result-index -1))
            close-autocomplete!
            (fn []
              (highlight-clear!))
            reset-state!
            (fn []
              (om/set-state! owner
                             {:text ""
                              :force-close? false
                              :highlighted-result-index -1}))
            send-message!
            (fn []
              (dispatch! :new-message {:thread-id (config :thread-id)
                                       :content text})
              (reset-state!))
            choose-result!
            (fn [result]
              ((result :action) (config :thread-id))
              (om/set-state! owner :text ((result :message-transform) text))
              (close-autocomplete!))
            autocomplete-open? (and (not force-close?) (not (nil? results)))]
          (dom/div #js {:className "message new"}
            (dom/textarea #js {:placeholder (config :placeholder)
                               :value (state :text)
                               :onChange (fn [e]
                                           (om/update-state! owner
                                                             (fn [s]
                                                               (assoc s
                                                                 :text (.. e -target -value)
                                                                 :force-close? false))))
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
                                           (close-autocomplete!)
                                           (om/set-state! owner :force-close? true))))
                                     ; ENTER otherwise -> send message
                                     (not e.shiftKey)
                                     (do
                                       (.preventDefault e)
                                       (send-message!)))

                                   KeyCodes.ESC (do
                                                  (om/set-state! owner :force-close? true)
                                                  (close-autocomplete!))

                                   KeyCodes.UP (when autocomplete-open?
                                                 (.preventDefault e)
                                                 (highlight-prev!))

                                   KeyCodes.DOWN (when autocomplete-open?
                                                   (.preventDefault e)
                                                   (highlight-next!))
                                   (when (KeyCodes.isTextModifyingKeyEvent e)
                                     ; don't clear if a modifier key alone was pressed
                                     (highlight-clear!))))})

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
                                                 (choose-result! result))}
                          ((result :html))))
                      results))
                  (dom/div #js {:className "result"}
                    "No Results")))))))))
