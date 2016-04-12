(ns chat.client.views
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.reagent-adapter :refer [reagent->react subscribe]]
            [chat.client.views.style :refer [style-view]]
            [braid.ui.views.sidebar :refer [sidebar-view]]
            [braid.ui.views.header :refer [header-view]]
            [braid.ui.views.login :refer [login-view]]
            [chat.client.views.pages.search :refer [search-page-view]]
            [chat.client.views.pages.inbox :refer [inbox-page-view]]
            [chat.client.views.pages.recent :refer [recent-page-view]]
            [chat.client.views.pages.channel :refer [channel-page-view]]
            [chat.client.views.pages.channels :refer [channels-page-view]]
            [chat.client.views.pages.users :refer [users-page-view]]
            [chat.client.views.pages.user :refer [user-page-view]]
            [chat.client.views.pages.extensions :refer [extensions-page-view]]
            [chat.client.views.pages.help :refer [help-page-view]]
            [chat.client.views.pages.group-explore :refer [group-explore-view]]
            [chat.client.views.pages.me :refer [me-page-view]]))

(def SidebarView
  (reagent->react sidebar-view))

(def HeaderView
  (reagent->react header-view))

(defn main-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "main"}
        (apply dom/div #js {:className "error-banners"}
          (for [[err-key err] (data :errors)]
            (dom/div #js {:className "error-banner"}
              err
              (dom/span #js {:className "close"
                             :onClick (fn [_] (store/clear-error! err-key))} "×"))))

         (SidebarView. #js {:subscribe subscribe})

        (HeaderView. #js {:subscribe subscribe})

        (case (get-in data [:page :type])
          :inbox (om/build inbox-page-view data)
          :recent (om/build recent-page-view data)
          :help (om/build help-page-view data)
          :users (om/build users-page-view data)
          :search (om/build search-page-view data)
          :channel (om/build channel-page-view data {:react-key (get-in data [:page :id])})
          :user (om/build user-page-view data)
          :channels (om/build channels-page-view data)
          :me (om/build me-page-view data)
          :group-explore (om/build group-explore-view data)
          :extensions (om/build extensions-page-view data))))))

(def StyleView
  (reagent->react style-view))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "app"}
        (StyleView.)
        (if (data :session)
          (om/build main-view data)
          (om/build login-view data))))))
