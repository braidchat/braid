(ns braid.ui.views.login
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]))

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
