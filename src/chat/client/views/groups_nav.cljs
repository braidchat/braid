(ns chat.client.views.groups-nav
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color]]))

(defn groups-nav-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "groups-nav"}

        (let [user-id (get-in @store/app-state [:session :user-id])]
          (dom/img #js {:className "avatar"
                        :style #js {:backgroundColor (id->color user-id)}
                        :onClick (fn [e]
                                   (store/set-page! {:type :me}))
                        :src (get-in @store/app-state [:users user-id :avatar])}))

        (apply dom/div #js {:className "groups"}
          (map (fn [group]
                 (dom/div #js {:className (str "group "
                                               (when (= (@store/app-state :open-group-id)  (group :id)) "active"))
                               :style #js {:backgroundColor (id->color (group :id))}
                               :onClick (fn [e]
                                          (store/set-open-group! (group :id))
                                          (store/set-page! {:type :inbox}))}
                   (string/join "" (take 2 (group :name)))))
               (vals (data :groups))))

        (dom/div #js {:className "plus"
                      :onClick (fn [_] (store/set-page! {:type :group-explore}))}
          "")))))
