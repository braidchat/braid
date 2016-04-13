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
      [:div.login
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (dispatch! :auth
                                       {:email (@state :email)
                                        :password (@state :password)
                                        :on-error (fn []
                                                    (set-error!))}))}

        [:fieldset
         [:label "Email"
          [:input {:placeholder "you@example.com"
                   :type "text"
                   :value (@state :email)
                   :on-change (fn [e] (set-email! (.. e -target -value)))}] ]

         [:label "Password"
          [:input {:placeholder "••••••••"
                   :type "password"
                   :value (@state :password)
                   :on-change (fn [e] (set-password! (.. e -target -value)))}] ]

         [:button.submit "Let's do this!"]

         (when (@state :error)
           [:div.error
            [:div.message
             (cond
               (not (seq (@state :email)))
               "Please enter an email."
               (not (seq (@state :password)))
               "Please enter a password."
               :else
               "Incorrect email or password. Please try again.")]

            (when (seq (@state :email))
              [:button.reset-password
               {:on-click (fn [e]
                            (.preventDefault e)
                            (dispatch! :request-reset (@state :email)))}
               "Send me a password reset email"])])]]])))
