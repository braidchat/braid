(ns braid.ui.views.login
  (:require [reagent.core :as r]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn login-view []
  (let [state (r/atom {:email ""
                       :password ""
                       :error false})
        set-error! (fn []
                     (swap! state assoc :error true))
        set-email! (fn [email]
                     (swap! state assoc :email email))
        set-password! (fn [password]
                        (swap! state assoc :password password))]

    (fn [_]
      [:form.login {:on-submit (fn [e]
                                 (.preventDefault e)
                                 (dispatch! :auth
                                            {:email (@state :email)
                                             :password (@state :password)
                                             :on-error (fn []
                                                         (set-error!))}))}

       (when (@state :error)
         [:div.error
          [:p "Bad credentials, please try again."]
          [:p [:a {:href "#"
                   :on-click (fn [e]
                               (.preventDefault e)
                               (dispatch! :request-reset (@state :email)))}
               "Request a password reset to be sent to " (@state :email)]]])

       [:input {:placeholder "Email"
                :type "text"
                :value (@state :email)
                :on-change (fn [e] (set-email! (.. e -target -value)))}]

       [:input {:placeholder "Password"
                :type "password"
                :value (@state :password)
                :on-change (fn [e] (set-password! (.. e -target -value)))}]

       [:button "Let's do this!"]])))
