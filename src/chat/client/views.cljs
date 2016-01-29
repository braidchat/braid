(ns chat.client.views
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.groups-nav :refer [groups-nav-view]]
            [chat.client.views.sidebar :refer [sidebar-view]]
            [chat.client.views.pages.search :refer [search-page-view]]
            [chat.client.views.pages.inbox :refer [inbox-page-view]]
            [chat.client.views.pages.channel :refer [channel-page-view]]
            [chat.client.views.pages.channels :refer [channels-page-view]]
            [chat.client.views.pages.user :refer [user-page-view]]
            [chat.client.views.pages.group-explore :refer [group-explore-view]]
            [chat.client.views.pages.me :refer [me-page-view]]
            [chat.client.views.search-bar :refer [search-bar-view]]
            [chat.client.views.pills :refer [tag-view user-view]]))

(defn login-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:email ""
       :password ""
       :error false})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "login"}
        (when (state :error)
          (dom/div #js {:className "error"}
            "Bad credentials, please try again"))
        (dom/input
          #js {:placeholder "Email"
               :type "text"
               :value (state :email)
               :onChange (fn [e] (om/set-state! owner :email (.. e -target -value)))})
        (dom/input
          #js {:placeholder "Password"
               :type "password"
               :value (state :password)
               :onChange (fn [e] (om/set-state! owner :password (.. e -target -value)))})
        (dom/button
          #js {:onClick (fn [e]
                          (dispatch! :auth
                                     {:email (state :email)
                                      :password (state :password)
                                      :on-error
                                      (fn []
                                        (om/set-state! owner :error true))}))}
          "Let's do this!")))))

(defn main-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "main"}
        (when-let [err (data :error-msg)]
          (dom/div #js {:className "error-banner"}
            err
            (dom/span #js {:className "close"
                           :onClick (fn [_] (store/clear-error!))} "×")))
        (om/build groups-nav-view data)
        (om/build sidebar-view data)

        (dom/div #js {:className "header"}
          (let [users (->> (@store/app-state :users)
                           vals
                           (remove (fn [user] (= (get-in @store/app-state [:session :user-id]) (user :id))))
                           (filter (fn [u] (contains? (set (u :group-ids)) (@store/app-state :open-group-id)))))
                users-online (->> users
                                  (filter (fn [user] (= :online (user :status)))))]
            (dom/div #js {:className "users"}
              (dom/div #js {:className "title" :title "Users"}
                (count users-online))
              (apply dom/div #js {:className "modal"}
                (dom/h2 nil "Online")
                (->> users-online
                     (map (fn [user]
                            (dom/div nil
                              (om/build user-view user))))))))
          (dom/div #js {:className "tags"}
            (dom/div #js {:className "title"
                          :title "Tags"
                          :onClick (fn [e] (store/set-page! {:type :channels}))})
            (dom/div #js {:className "modal"}
              (apply dom/div nil
                (->> (@store/app-state :tags)
                     vals
                     (filter (fn [t] (= (@store/app-state :open-group-id) (t :group-id))))
                     (filter (fn [t] (store/is-subscribed-to-tag? (t :id))))
                     (sort-by :threads-count)
                     reverse
                     (map (fn [tag]
                            (dom/div nil (om/build tag-view tag))))))))
          (dom/div #js {:className "help"}
            (dom/div #js {:className "title" :title "Help"})
            (dom/div #js {:className "modal"}
              (dom/p nil "Conversations must be tagged to be seen by other people.")
              (dom/p nil "Tag a conversation by mentioning a tag in a message: ex. #general")
              (dom/p nil "You can also mention users to add them to a conversation: ex. @raf")
              (dom/p nil "Add emoji by using :shortcodes: (they autocomplete).")))

          (om/build search-bar-view (data :page)))

        (case (get-in data [:page :type])
          :inbox (om/build inbox-page-view data)
          :search (om/build search-page-view data)
          :channel (om/build channel-page-view data {:react-key (get-in data [:page :id])})
          :user (om/build user-page-view data)
          :channels (om/build channels-page-view data)
          :me (om/build me-page-view data)
          :group-explore (om/build group-explore-view data))))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "app"}
        (if (data :session)
          (om/build main-view data)
          (om/build login-view data))))))
