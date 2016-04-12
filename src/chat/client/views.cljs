(ns chat.client.views
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.reagent-adapter :refer [reagent->react subscribe]]
            [chat.client.views.style :refer [style-view]]
            [braid.ui.views.sidebar :refer [sidebar-view]]
            [braid.ui.views.header :refer [header-view]]
            [braid.ui.views.pages.channel :refer [channel-page-view-test]]
            [braid.ui.views.pages.help :refer [help-page-view-test]]
            [braid.ui.views.pages.me :refer [me-page-view-test]]
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

(defn login-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:email ""
       :password ""
       :error false})
    om/IRenderState
    (render-state [_ state]
      (dom/form #js {:className "login"
                     :onSubmit (fn [e]
                                 (.preventDefault e)
                                 (dispatch! :auth
                                            {:email (state :email)
                                             :password (state :password)
                                             :on-error
                                             (fn []
                                               (om/set-state! owner :error true))}))}
        (when (state :error)
          (dom/div #js {:className "error"}
            (dom/p nil "Bad credentials, please try again")
            (dom/p nil
              (dom/a #js {:href "#"
                          :onClick (fn [e]
                                     (.preventDefault e)
                                     (dispatch! :request-reset (state :email)))}
                (str "Request a password reset to be sent to "
                     (state :email))))))
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
        (dom/button nil "Let's do this!")))))

(def SidebarView
  (reagent->react sidebar-view))

(def HeaderView
  (reagent->react header-view))

(def HelpPageView
  (reagent->react help-page-view-test))

(def MePageView
  (reagent->react me-page-view-test))

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
          :help (HelpPageView. #js {:data data})
          :users (om/build users-page-view data)
          :search (om/build search-page-view data)
          :channel (ChannelPageView. #js {:subscribe subscribe
                                          :data data})
          :user (om/build user-page-view data)
          :channels (om/build channels-page-view data)
          :me (MePageView. #js {:data data})
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
