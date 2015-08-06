(ns chat.client.views.new-message
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]))

(defn new-message-view [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:text ""
       :highlighted-result-index -1 })
    om/IRenderState
    (render-state [_ {:keys [text highlighted-result-index] :as state}]
      (let [clear-tags! (fn []
                          (om/set-state! owner :text (string/replace text #"( *#\S*)" "")))
            constrain (fn [x a z]
                        (cond
                          (> x z) z
                          (< x a) a
                          :else x))
            thread-tag-ids (-> (store/id->thread (config :thread-id))
                               :tag-ids
                               set)
            partial-tag (second (re-matches #".*#(.*)" text))
            results (->> (store/all-tags)
                         (remove (fn [t]
                                   (contains? thread-tag-ids (t :id))))
                         (filter (fn [t]
                                   (re-matches (re-pattern (str ".*" partial-tag ".*")) (t :name)))))
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
            autocomplete-open? (not (nil? partial-tag))]
          (dom/div #js {:className "message new"}
            (dom/textarea #js {:placeholder (config :placeholder)
                               :value (state :text)
                               :onChange (fn [e]
                                           (om/set-state! owner :text (.. e -target -value)))
                               :onKeyDown
                               (fn [e]
                                 (cond
                                   ; enter when autocomplete -> tag
                                   (and (= 13 e.keyCode) autocomplete-open?)
                                   (do
                                     (.preventDefault e)
                                     (when-let [tag (nth results highlighted-result-index nil)]
                                       (dispatch! :tag-thread {:thread-id (config :thread-id)
                                                               :id (tag :id)})
                                       (close-autocomplete!)))
                                   ; enter otherwise -> message
                                   (and (= 13 e.keyCode) (= e.shiftKey false))
                                   (do
                                     (.preventDefault e)
                                     (dispatch! :new-message {:thread-id (config :thread-id)
                                                              :content text})
                                     (clear-text!))
                                   ; esc
                                   (= 27 e.keyCode)
                                   (do
                                     (close-autocomplete!))
                                   ; up
                                   (= 38 e.keyCode)
                                   (do
                                     (.preventDefault e)
                                     (highlight-prev!))
                                   ; down
                                   (= 40 e.keyCode)
                                   (do
                                     (.preventDefault e)
                                     (highlight-next!))
                                   :else
                                   (do
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
                                                 (dispatch! :tag-thread {:thread-id (config :thread-id)
                                                                         :id (tag :id)})
                                                 (close-autocomplete!))}
                          (dom/div #js {:className "tag-name"}
                            (tag :name))
                          (dom/div #js {:className "group-name"}
                            (:name (store/id->group (tag :group-id))))))
                      results))
                  (dom/div #js {:className "result"}
                    "No Results")))))))))
