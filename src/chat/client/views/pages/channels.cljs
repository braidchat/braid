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

(defn group-tags-view [[group-id tags] owner opts]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "group"}
        (dom/h2 #js {:className "name"}
          (:name (store/id->group group-id)))
        (om/build new-tag-view {:group-id group-id} {:opts opts})
        (apply dom/div #js {:className "tags"}
          (map (fn [tag]
                 (dom/div #js {:className "tag-info"}
                   (dom/span #js {:className "count threads-count"}
                     (tag :threads-count))
                   (dom/span #js {:className "count subscribers-count"}
                     (tag :subscribers-count))
                   (om/build tag-view tag)
                   (subscribe-button tag)))
              tags))))))

(defn groups-view [grouped-tags owner opts]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "groups"}
        (om/build-all group-tags-view grouped-tags {:opts opts})))))

(defn channels-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page channels"}
        (dom/div #js {:className "title"}
          "Channels")

        (dom/div #js {:className "content"}
          (let [groups-map (->> (keys (data :groups))
                                (into {} (map (juxt identity (constantly nil))) ))
                ; groups-map is just map of group-ids to nil, to be merged with
                ; tags, so there is still an entry for groups without any tags
                grouped-tags (->> (data :tags)
                                  vals
                                  (map (fn [tag]
                                         (assoc tag :subscribed?
                                           (store/is-subscribed-to-tag? (tag :id)))))
                                  (sort-by :threads-count)
                                  reverse
                                  (group-by :group-id)
                                  (merge groups-map))]
            (om/build groups-view grouped-tags)))))))
