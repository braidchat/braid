(ns chat.client.views.pages.users
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.store :as store]
            [chat.client.views.group-invite :refer [group-invite-view]]))

(defn- group-users [data]
  (->> (data :users)
       vals
       (filter (fn [u] (some #{(data :open-group-id)} (u :group-ids))))))

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/li nil
          (dom/a #js {:href (routes/user-page-path {:group-id (routes/current-group)
                                                    :user-id (user :id)})}
            (dom/img #js {:className "avatar"
                          :style #js {:backgroundColor (id->color (user :id))}
                          :src (user :avatar)})
            (dom/p nil (user :nickname))))))))

(defn user-list-view [users owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/ul nil
        (om/build-all user-view users)))))

(defn users-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page users"}
        (dom/div #js {:className "title"} "Users")
        (dom/div #js {:className "content"}
          (let [users-by-status (->> (group-users data)
                                     (group-by :status))]
            (dom/div #js {:className "description"}
              (dom/h2 nil "Online")
              (om/build user-list-view (users-by-status :online))

              (dom/h2 nil "Offline")
              (om/build user-list-view (users-by-status :offline))))

          (dom/h2 nil "Invite")
          (om/build group-invite-view (data :open-group-id)))))))
