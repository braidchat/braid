(ns braid.core.client.gateway.forms.user-auth.views
  (:require
   [braid.core.client.gateway.helper-views :refer [form-view field-view]]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch dispatch-sync subscribe]]))

(defn auth-providers-view []
  [:span.auth-providers
   (doall
    (for [provider [:github :google]]
      ^{:key provider}
      [:button
       {:type "button"
        :class (name provider)
        :on-click (fn [e]
                    (.preventDefault e)
                    ;; must use dispatch-sync
                    ;; b/c dispatch triggers pop-up blocker
                    (dispatch-sync [:braid.core.client.gateway.forms.user-auth.events/open-oauth-window provider]))}
       (string/capitalize (name provider))]))])

(defn returning-email-field-view []
  [field-view
   {:id :email
    :subs-ns :braid.core.client.gateway.forms.user-auth.subs
    :disp-ns :braid.core.client.gateway.forms.user-auth.events
    :title "Email"
    :class "email"
    :type "email"
    :placeholder "you@awesome.com"
    :auto-complete true
    :auto-focus true
    :help-text [:span "Or, log in with: " [auth-providers-view]]}])

(defn returning-password-field-view []
  [field-view
   {:id :password
    :subs-ns :braid.core.client.gateway.forms.user-auth.subs
    :disp-ns :braid.core.client.gateway.forms.user-auth.events
    :title "Password"
    :class "password"
    :type "password"
    :placeholder "•••••••••••"
    :help-text [:span "Don't remember?"
                [:button
                 {:type "button"
                  :on-click (fn [e]
                              (.preventDefault e)
                              (dispatch [:braid.core.client.gateway.forms.user-auth.events/set-mode :request-password-reset]))}
                 "Request a Password Reset"]]}])

(defn login-button-view []
  (let [fields-valid?
        @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/fields-valid?
                     [:email
                      :password]])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     "Log in to Braid"]))

(defn error-view []
  (let [error @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/error])]
    (when error
      (case error
        :auth-fail
        [:div.error-message
         "The email or password is incorrect. Please try again."]
        :email-exists
        [:div.error-message
         "A user is already registered with that email."
         [:button
          {:type "button"
           :on-click (fn [e]
                       (.preventDefault e)
                       (dispatch [:braid.core.client.gateway.forms.user-auth.events/set-mode :log-in]))}
          "Log In"]]
        :no-such-email
        [:div.error-message
         "An account with that email does not exist."]

        :password-reset-email-sent
        [:div.message
         "A password recovery email was sent. Please check your inbox."]

        ; catch-all
        [:div.error-message
         "An error occured. Please try again."]))))

(defn password-reset-button-view []
  (let [fields-valid?
        @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/fields-valid?
                     [:email]])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     "Request a Password Reset"]))

(defn request-password-reset-view []
  [form-view
   {:title "Request a Password Reset"
    :class "user-auth request-password-reset"
    :on-submit (fn [e]
                 (.preventDefault e)
                 (dispatch [:braid.core.client.gateway.forms.user-auth.events/submit-form
                            {:validate-fields [:email]
                             :dispatch-when-valid [:braid.core.client.gateway.forms.user-auth.events/remote-request-password-reset]}]))}
   [:p
    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:braid.core.client.gateway.forms.user-auth.events/set-mode :register]))}
     "Register"]

    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:braid.core.client.gateway.forms.user-auth.events/set-mode :register]))}
     "Log In"]]

   [returning-email-field-view]
   [password-reset-button-view]
   [error-view]])

(defn returning-user-view []
  [form-view
   {:title "Log in to Braid"
    :class "user-auth returning-user"
    :on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:braid.core.client.gateway.forms.user-auth.events/submit-form
                 {:validate-fields [:email
                                    :password]
                  :dispatch-when-valid [:braid.core.client.gateway.forms.user-auth.events/remote-log-in]}]))}
   [:p "Don't have an account?"
    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:braid.core.client.gateway.forms.user-auth.events/set-mode :register]))}
     "Register"]]
   [returning-email-field-view]
   [returning-password-field-view]
   [login-button-view]
   [error-view]])

(defn new-password-field-view []
  [field-view
   {:id :new-password
    :subs-ns :braid.core.client.gateway.forms.user-auth.subs
    :disp-ns :braid.core.client.gateway.forms.user-auth.events
    :title "Password"
    :class "password"
    :type "password"
    :placeholder "•••••••••••"
    :help-text "At least 8 characters. More is better!"}])

(defn new-email-field-view []
  [field-view
   {:id :email
    :subs-ns :braid.core.client.gateway.forms.user-auth.subs
    :disp-ns :braid.core.client.gateway.forms.user-auth.events
    :title "Email"
    :class "email"
    :type "email"
    :auto-complete true
    :auto-focus true
    :placeholder "you@awesome.com"
    :help-text [:span "Or, register with: " [auth-providers-view]]}])

(defn register-button-view []
  (let [fields-valid?
        @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/fields-valid?
                     [:email
                      :new-password]])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     "Create a Braid Account"]))

(defn new-user-view []
  [form-view
   {:title "Create a Braid Account"
    :class "user-auth new-user"
    :on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:braid.core.client.gateway.forms.user-auth.events/submit-form
                 {:validate-fields [:email
                                    :new-password]
                  :dispatch-when-valid [:braid.core.client.gateway.forms.user-auth.events/remote-register]}]))}
   [:p "Already have one?"
    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:braid.core.client.gateway.forms.user-auth.events/set-mode :log-in]))}
     "Log In"]]
   [new-email-field-view]
   [new-password-field-view]
   [register-button-view]
   [error-view]])

(defn authed-user-view []
  (let [user @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/user])]
    [:div.section.user-auth.authed-user
     [:div.profile
      [:img.avatar {:src (user :avatar)}]
      [:div.info
       [:div.nickname "@" (user :nickname)]
       [:div.email (user :email)]]]
     [:p "Not you?"
      [:button
       {:type "button"
        :on-click (fn []
                    (dispatch [:braid.core.client.gateway.forms.user-auth.events/switch-account]))}
       "Sign in with a different account"]]]))

(defn checking-user-view []
  [:div.section.user-auth.checking
   [:div
    [:span "Authenticating..."]]])

(defn oauth-in-progress-view []
  [:div.section.user-auth.authorizing
   [:div
    [:span "Authenticating with " (string/capitalize (name @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/oauth-provider]))) "..."]]])

(defn user-auth-view []
  (case @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/user-auth-mode])
    :checking [checking-user-view]
    :register [new-user-view]
    :request-password-reset [request-password-reset-view]
    :log-in [returning-user-view]
    :authed [authed-user-view]
    :oauth-in-progress [oauth-in-progress-view]))
