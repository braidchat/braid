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
            [chat.client.views.pages.me :refer [me-page-view]]))

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
        (dom/div #js {:className "instructions"}
          "Tag conversations with #... Add/mention users with @... Emoji :shortcode: support")
        (om/build groups-nav-view data)
        (om/build sidebar-view data)
        (case (get-in data [:page :type])
          :inbox (om/build inbox-page-view data)
          :search (om/build search-page-view data)
          :channel (om/build channel-page-view data {:react-key (get-in data [:page :id])})
          :user (om/build user-page-view data)
          :channels (om/build channels-page-view data)
          :me (om/build me-page-view data))))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "app"}
        (if (data :session)
          (om/build main-view data)
          (om/build login-view data))))))
