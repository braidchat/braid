(ns chat.client.views.new-message
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.views.helpers :as helpers]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store])
  (:import [goog.events KeyCodes]))

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
;       if a trigger pattern was matched, an array of maps, each containing:
;         :html - html to be displayed for the result
;         :action - fn to be triggered when result picked
;         (this array may be empty)
;       otherwise nil


(def engines
  [
   (fn [text thread-id]
     (when-let [query (second (re-matches #"(?:.|\n)*#(\S*)" text))]
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
                      :html
                      (dom/div #js {:className "tag-match"}
                        (dom/div #js {:className "color-block"
                                      :style #js {:backgroundColor (helpers/tag->color tag)}})
                        (dom/div #js {:className "tag-name"}
                          (tag :name))
                        (dom/div #js {:className "group-name"}
                          (:name (store/id->group (tag :group-id)))))}))))))])


; TODO: autocomplete mentions
(defn new-message-view [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:text ""
       :highlighted-result-index -1})
    om/IRenderState
    (render-state [_ {:keys [text highlighted-result-index] :as state}]
      (let [clear-tags! (fn []
                          (om/set-state! owner :text (string/replace text #"( *#\S*)" "")))
            constrain (fn [x a z]
                        (cond
                          (> x z) z
                          (< x a) a
                          :else x))
            results ((get-in engines [0]) text (config :thread-id))
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
            clear-text!
            (fn []
              (om/set-state! owner :text ""))
            close-autocomplete!
            (fn []
              (clear-tags!)
              (highlight-clear!))
            autocomplete-open? (not (nil? results))]
          (dom/div #js {:className "message new"}
            (dom/textarea #js {:placeholder (config :placeholder)
                               :value (state :text)
                               :onChange (fn [e]
                                           (om/set-state! owner :text (.. e -target -value)))
                               :onKeyDown
                               (fn [e]
                                 (condp = e.keyCode
                                   KeyCodes.ENTER
                                   (cond
                                     ; enter when autocomplete -> tag
                                     autocomplete-open?
                                     (do
                                       (.preventDefault e)
                                       (when-let [tag (nth results highlighted-result-index nil)]
                                         ((tag :action) (config :thread-id))
                                         (close-autocomplete!)))
                                     ; enter otherwise -> message
                                     (not e.shiftKey)
                                     (do
                                       (.preventDefault e)
                                       (dispatch! :new-message {:thread-id (config :thread-id)
                                                                :content text})
                                       (clear-text!)))

                                   KeyCodes.ESC (close-autocomplete!)

                                   KeyCodes.UP (when autocomplete-open?
                                                 (.preventDefault e)
                                                 (highlight-prev!))

                                   KeyCodes.DOWN (when autocomplete-open?
                                                   (.preventDefault e)
                                                   (highlight-next!))
                                   (when (KeyCodes.isTextModifyingKeyEvent e)
                                     ; Don't clear if a modifier key alone was pressed
                                     (highlight-clear!))))})

            (when autocomplete-open?
              (dom/div #js {:className "autocomplete"}
                (if (seq results)
                  (apply dom/div nil
                    (map-indexed
                      (fn [i tag]
                        (dom/div #js {:className (str "result" " "
                                                      (when (= i highlighted-result-index) "highlight"))
                                      :style #js {:cursor "pointer"}
                                      :onClick (fn []
                                                 ((tag :action) (config :thread-id))
                                                 (close-autocomplete!))}
                          (tag :html)))
                      results))
                  (dom/div #js {:className "result"}
                    "No Results")))))))))
