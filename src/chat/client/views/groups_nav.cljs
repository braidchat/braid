(ns chat.client.views.groups-nav
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]))

(defn- unseen? [message thread]
  (> (:created-at message)
     (thread :last-open-at)))

(defn groups-nav-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "groups-nav"}
        (let [group-new-activity
              (->>
                (select-keys (data :threads) (get-in data [:user :open-thread-ids]))
                vals
                (filter (fn [thread]
                          (unseen? (->> (thread :messages)
                                        (sort-by :created-at)
                                        last)
                                   thread)))
                (mapcat (fn [thread]
                          (set (flatten (concat (->> (thread :tag-ids)
                                                     (map (fn [tag-id]
                                                            (get-in @store/app-state [:tags tag-id :group-id]))))
                                                (->> (thread :mentioned-ids)
                                                     (map (fn [user-id]
                                                            (get-in @store/app-state [:users user-id :group-ids])))))))))
                frequencies)]
          (apply dom/div #js {:className "groups"}
            (map (fn [group]
                   (dom/a #js {:className (str "option group "
                                               (when (= (@store/app-state :open-group-id)  (group :id)) "active"))
                               :style #js {:backgroundColor (id->color (group :id))}
                               :title (group :name)
                               :href (routes/page-path {:group-id (group :id)
                                                        :page-id "inbox"})}
                     (string/join "" (take 2 (group :name)))
                     (when-let [cnt (group-new-activity (group :id))]
                       (dom/span #js {:className "badge"} cnt))))
                 (vals (data :groups)))))

        (dom/a #js {:className (str "option plus "
                                    (when (= (get-in @store/app-state [:page :type]) :group-explore) "active"))
                    :href (routes/other-path {:page-id "group-explore"})} "")))))
