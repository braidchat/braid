(ns chat.client.views.header
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.search-bar :refer [search-bar-view]]
            [chat.client.views.pills :refer [tag-view user-view]]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn clear-inbox-button [_ owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:onClick (fn [_]
                              (dispatch! :clear-inbox))} "Clear Inbox"))))

(defn header-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "header"}

        (om/build clear-inbox-button {})

        (let [path (routes/inbox-page-path {:group-id (routes/current-group)})]
          (dom/div #js {:className (str "inbox shortcut "
                                        (when (routes/current-path? path) "active"))}
            (dom/a #js {:href path
                        :className "title"
                        :title "Inbox"})))

        (let [path (routes/recent-page-path {:group-id (routes/current-group)})]
          (dom/div #js {:className (str "recent shortcut "
                                        (when (routes/current-path? path) "active"))}
            (dom/a #js {:href path
                        :className "title"
                        :title "Recent"})))

        (let [users (->> (store/users-in-open-group)
                         (remove (fn [user] (= (get-in @store/app-state [:session :user-id]) (user :id)))))
              users-online (->> users
                                (filter (fn [user] (= :online (user :status)))))
              path (routes/users-page-path {:group-id (routes/current-group)})]
          (dom/div #js {:className (str "users shortcut "
                                        (when (routes/current-path? path) "active"))}
            (dom/a #js {:href path
                        :className "title"
                        :title "Users"}
              (count users-online))
            (apply dom/div #js {:className "modal"}
              (dom/h2 nil "Online")
              (->> users-online
                   (map (fn [user]
                          (dom/div nil
                            (om/build user-view user))))))))

        (let [path (routes/page-path {:group-id (routes/current-group)
                                      :page-id "channels"})]
          (dom/div #js {:className (str "tags shortcut "
                                        (when (routes/current-path? path) "active"))}
            (dom/a #js {:href path
                        :className "title"
                        :title "Tags"})
            (dom/div #js {:className "modal"}
              (apply dom/div nil
                (->> (@store/app-state :tags)
                     vals
                     (filter (fn [t] (= (@store/app-state :open-group-id) (t :group-id))))
                     (filter (fn [t] (store/is-subscribed-to-tag? (t :id))))
                     (sort-by :threads-count)
                     reverse
                     (map (fn [tag]
                            (dom/div nil (om/build tag-view tag)))))))))

        (comment
          (let [path (routes/extensions-page-path {:group-id (routes/current-group)})]
            (dom/div #js {:className (str "extensions shortcut "
                                          (when (routes/current-path? path) "active"))}
              (dom/a #js {:href path
                          :className "title"
                          :title "Extensions"})
              (dom/div #js {:className "modal"}
                (dom/div nil "extensions")))))

        (let [path (routes/help-page-path {:group-id (routes/current-group)})]
          (dom/div #js {:className (str "help shortcut "
                                        (when (routes/current-path? path) "active"))}
            (dom/a #js {:href path
                        :className "title"
                        :title "Help"})
            (dom/div #js {:className "modal"}
              (dom/p nil "Conversations must be tagged to be seen by other people.")
              (dom/p nil "Tag a conversation by mentioning a tag in a message: ex. #general")
              (dom/p nil "You can also mention users to add them to a conversation: ex. @raf")
              (dom/p nil "Add emoji by using :shortcodes: (they autocomplete)."))))

        (om/build search-bar-view (data :page))

        (let [user-id (get-in @store/app-state [:session :user-id])
              path (routes/page-path {:group-id (routes/current-group)
                                      :page-id "me"})]
          (dom/a #js {:href path
                      :className (when (routes/current-path? path) "active")}
            (dom/img #js {:className "avatar"
                          :style #js {:backgroundColor (id->color user-id)}
                          :src (get-in @store/app-state [:users user-id :avatar])})))))))
