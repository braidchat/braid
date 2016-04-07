(ns chat.client.views.header
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.search-bar :refer [search-bar-view]]
            [chat.client.views.pills :refer [tag-view user-view]]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn header-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "header"}

        (comment
          (let [path (routes/extensions-page-path {:group-id (routes/current-group)})]
            (dom/div #js {:className (str "extensions shortcut "
                                          (when (routes/current-path? path) "active"))}
              (dom/a #js {:href path
                          :className "title"
                          :title "Extensions"})
              (dom/div #js {:className "modal"}
                (dom/div nil "extensions")))))

        (om/build search-bar-view (data :page))

        (let [user-id (get-in @store/app-state [:session :user-id])
              path (routes/page-path {:group-id (routes/current-group)
                                      :page-id "me"})]
          (dom/a #js {:href path
                      :className (when (routes/current-path? path) "active")}
            (dom/img #js {:className "avatar"
                          :style #js {:backgroundColor (id->color user-id)}
                          :src (get-in @store/app-state [:users user-id :avatar])})))))))
