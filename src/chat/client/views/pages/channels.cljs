(ns chat.client.views.pages.channels
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.pills :refer [tag-view subscribe-button]]
            [chat.shared.util :refer [valid-tag-name? valid-nickname?]])
  (:import [goog.events KeyCodes]))

(defn new-tag-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:error nil})
    om/IRenderState
    (render-state [_ state]
      (dom/input #js {:className (str "new-tag " (when (state :error) "error"))
                      :onKeyUp
                      (fn [e]
                        (let [text (.. e -target -value)]
                          (om/set-state! owner :error (not (valid-tag-name? text)))))
                      :onKeyDown
                      (fn [e]
                        (when (= KeyCodes.ENTER e.keyCode)
                          (let [text (.. e -target -value)]
                            (dispatch! :create-tag [text (data :group-id)]))
                          (.preventDefault e)
                          (aset (.. e -target) "value" "")))
                      :placeholder "New Tag"}))))

(defn tag-info-view [tag owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "tag-info"}
        (dom/span #js {:className "count threads-count"}
          (tag :threads-count))
        (dom/span #js {:className "count subscribers-count"}
          (tag :subscribers-count))
        (om/build tag-view tag)
        (om/build subscribe-button tag)))))

(defn channels-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [group-id (data :open-group-id)
            tags (->> (data :tags)
                      vals
                      (filter (fn [t] (= group-id (t :group-id))))
                      (sort-by :threads-count)
                      reverse)]
        (dom/div #js {:className "page channels"}
          (dom/div #js {:className "title"} "Tags")

          (dom/div #js {:className "content"}
            (om/build new-tag-view {:group-id group-id})

            (let [subscribed-tags
                  (->> tags
                       (filter (fn [t] (store/is-subscribed-to-tag? (t :id)))))]
              (when (seq subscribed-tags)
                (dom/div nil
                  (dom/h2 nil "Subscribed")
                  (apply dom/div #js {:className "tags"}
                    (map (fn [tag]
                           (om/build tag-info-view tag {:react-key (tag :id)}))
                         subscribed-tags)))))

              (let [recommended-tags
                    (->> tags
                         ; TODO actually use some interesting logic here
                         (remove (fn [t] (store/is-subscribed-to-tag? (t :id)))))]
                (when (seq recommended-tags)
                  (dom/div nil
                    (dom/h2 nil "Recommended")
                    (apply dom/div #js {:className "tags"}
                      (map (fn [tag]
                             (om/build tag-info-view tag {:react-key (tag :id)}))
                           recommended-tags)))))))))))
