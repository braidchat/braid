(ns braid.client.ui.views.login
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch]]))

(defn login-view []
  (let [state (r/atom {:email ""
                       :password ""
                       :error nil
                       :loading? false})
        set-error! (fn [message]
                     (swap! state assoc :error message))
        set-email! (fn [email]
                     (swap! state assoc :email email))
        set-password! (fn [password]
                        (swap! state assoc :password password))
        set-loading! (fn [bool]
                       (swap! state assoc :loading? bool))]

    (fn [_]
      [:div.login
       [:div
        [:form {:on-submit
                (fn [e]
                  (.preventDefault e)
                  (set-error! nil)
                  (cond
                    (not (seq (@state :email)))
                    (set-error! "Please enter an email.")

                    (not (seq (@state :password)))
                    (set-error! "Please enter a password.")

                    :else
                    (do
                      (set-loading! true)
                      (dispatch [:auth
                                 {:email (@state :email)
                                  :password (@state :password)
                                  :on-complete (fn []
                                                 (set-loading! false))
                                  :on-error
                                  (fn []
                                    (set-loading! false)
                                    (set-error! (str "Incorrect email or password. "
                                                     " Please try again.")))}]))))}

         [:fieldset
          [:label "Email"
           [:input {:placeholder "you@example.com"
                    :type "email"
                    :value (@state :email)
                    :on-change (fn [e] (set-email! (.. e -target -value)))}] ]

          [:label "Password"
           [:input {:placeholder "••••••••"
                    :type "password"
                    :value (@state :password)
                    :on-change (fn [e] (set-password! (.. e -target -value)))}] ]

          [:button.submit "Let's do this!"]

          (when (@state :loading?)
            [:div.spinner])

          (when (@state :error)
            [:div.error
             [:div.message (@state :error)]

             (when (seq (@state :email))
               [:button.reset-password
                {:on-click (fn [e]
                             (.preventDefault e)
                             (dispatch [:request-reset (@state :email)]))}
                "Send me a password reset email"])])]]
        [:div.alternatives
         "Or"
         [:div.github
          [:button {:on-click (fn [_] (set! (.-location js/window) "/github-login"))}
           "Login with Github"]]]]])))
