(ns chat.client.views.pages.users
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.store :as store]
            [chat.client.views.group-invite :refer [group-invite-view]]))


;; Helper Functions

(defn group-users [data]
  (filter  #(some #{(data :open-group-id)} (get-in % [1 :group-ids]))
           (data :users)))

(defn status-filter [users status]
  (filter #(= status (get-in % [1 :status]))
          users))


;;; USER VIEW

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/li nil

                       (dom/a #js {:href (routes/user-page-path {:group-id (routes/current-group)
                                                                 :user-id (get-in user [1 :id])})}
                              (dom/img #js{:className "avatar"
                                           :style #js {:backgroundColor (id->color (get-in user [1 :id]))}
                                           :src (get-in @store/app-state [:users (get-in user [1 :id]) :avatar])})
                              (dom/p nil (get-in user [1 :nickname]))))))))


;;; User list view

(defn user-list-view [users owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/ul nil
             (om/build-all user-view users)))))



;;; All group users

(defn group-users-view [group-users owner]
    (reify
      om/IRender
      (render [_]
        (dom/div nil
                 (dom/h2 nil "Users")
                 (dom/div nil
                          (dom/h3 nil "Online")
                          (om/build user-list-view (status-filter group-users :online))

                          (dom/h3 nil "Offline")
                          (om/build user-list-view (status-filter group-users :offline))

                          )))))



(defn users-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page users"}
        (dom/div #js {:className "title"} "Users")

        (dom/div #js {:className "content"}

                 (dom/h2 nil "Send Invites")
                 (dom/div nil
                          (dom/div #js {}
                                   (dom/div nil
                                            (:name
                                             (first
                                              (filter #(= (data :open-group-id) (% :id))
                                                      (vals (data :groups))))))
                                   (om/build group-invite-view (data :open-group-id)))))


        (dom/div #js {:className "content"}
                 (dom/div #js {:className "description"}
                          (om/build group-users-view (group-users data))))))))
