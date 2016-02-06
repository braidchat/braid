(ns chat.client.views.groups-nav
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]))

(defn groups-nav-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "groups-nav"}

        (apply dom/div #js {:className "groups"}
          (map (fn [group]
                 (dom/a #js {:className (str "option group "
                                             (when (= (@store/app-state :open-group-id)  (group :id)) "active"))
                             :style #js {:backgroundColor (id->color (group :id))}
                             :title (group :name)
                             :href (routes/page-path {:group-id (group :id)
                                                      :page-id "inbox"})}
                   (string/join "" (take 2 (group :name)))
                   (let [cnt (->>
                               (select-keys (data :threads) (get-in data [:user :open-thread-ids]))
                               vals
                               (filter (fn [thread]
                                         (contains? (set (->> (thread :tag-ids)
                                                              (map  (fn [tag-id]
                                                                      (get-in @store/app-state [:tags tag-id :group-id]))))) (group :id))))
                               count)]
                     (when (< 0 cnt)
                       (dom/span #js {:className "count"} cnt)))))
               (vals (data :groups))))

        (dom/a #js {:className (str "option plus "
                                    (when (= (get-in @store/app-state [:page :type]) :group-explore) "active"))
                    :href (routes/other-path {:page-id "group-explore"})} "")))))
