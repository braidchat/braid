(ns braid.client.gateway.user-auth.views
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]))

(defn auth-providers-view []
  [:span.auth-providers
   (doall
     (for [provider [:github :google :facebook]]
       ^{:key provider}
       [:button
        {:type "button"
         :class (name provider)
         :on-click (fn [e]
                     (.preventDefault e)
                     (dispatch [:gateway.user-auth/remote-oauth provider]))}
        (string/capitalize (name provider))]))])

(defn returning-email-field-view []
  (let [field-id :gateway.user-auth/email
        value @(subscribe [:gateway.user-auth/field-value field-id])
        status @(subscribe [:gateway.user-auth/field-status field-id])
        errors @(subscribe [:gateway.user-auth/field-errors field-id])]
    [:div.option.email
     {:class (name status)}
     [:h2 "Email"]
     [:label
      [:div.field
       [:input {:type "email"
                :placeholder "you@awesome.com"
                :auto-complete true
                :auto-correct "off"
                :auto-capitalize "off"
                :spell-check "false"
                :auto-focus true
                :value value
                :on-blur (fn [_]
                           (dispatch [:gateway.user-auth/blur field-id]))
                :on-change (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:gateway.user-auth/update-value field-id value])))
                }]]
      (when (= :invalid status)
        [:div.error-message (first errors)])
      ; TODO
      #_[:p "Or, log in with: "
         [auth-providers-view]]]]))

(defn returning-password-field-view []
  (let [field-id :gateway.user-auth/password
        value @(subscribe [:gateway.user-auth/field-value field-id])
        status @(subscribe [:gateway.user-auth/field-status field-id])
        errors @(subscribe [:gateway.user-auth/field-errors field-id])]
    [:div.option.password
     {:class (name status)}
     [:h2 "Password"]
     [:label
      [:div.field
       [:input {:type "password"
                :placeholder "•••••••••••"
                :value value
                :on-blur (fn [_]
                           (dispatch [:gateway.user-auth/blur field-id]))
                :on-change (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:gateway.user-auth/update-value field-id value])))}]]]
     (when (= :invalid status)
       [:div.error-message (first errors)])
     [:p "Don't remember?"
      [:button
       {:type "button"
        :on-click (fn [e]
                    (.preventDefault e)
                    (dispatch [:gateway.user-auth/set-mode :request-password-reset]))}
       "Request a Password Reset"]]]))

(defn login-button-view []
  (let [fields-valid?
        @(subscribe [:gateway.user-auth/fields-valid?
                     [:gateway.user-auth/email
                      :gateway.user-auth/password]])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     "Log in to Braid"]))

(defn error-view []
  (let [error @(subscribe [:gateway.user-auth/error])]
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
                       (dispatch [:gateway.user-auth/set-mode :log-in]))}
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
        @(subscribe [:gateway.user-auth/fields-valid?
                     [:gateway.user-auth/email]])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     "Request a Password Reset"]))

(defn request-password-reset-view []
  [:form.request-password-reset
   {:no-validate true
    :on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:gateway.user-auth/submit-form
                 {:validate-fields [:gateway.user-auth/email]
                  :dispatch-when-valid [:gateway.user-auth/remote-request-password-reset]}]))}
   [:h1 "Request a Password Reset"]
   [:p
    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:gateway.user-auth/set-mode :register]))}
     "Register"]

    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:gateway.user-auth/set-mode :register]))}
     "Log In"]]
   [returning-email-field-view]
   [password-reset-button-view]
   [error-view]])

(defn returning-user-view []
  [:form.returning-user
   {:no-validate true
    :on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:gateway.user-auth/submit-form
                 {:validate-fields [:gateway.user-auth/email
                                    :gateway.user-auth/password]
                  :dispatch-when-valid [:gateway.user-auth/remote-log-in]}]))}
   [:h1 "Log in to Braid"]
   [:p "Don't have an account?"
    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:gateway.user-auth/set-mode :register]))}
     "Register"]]
   [returning-email-field-view]
   [returning-password-field-view]
   [login-button-view]
   [error-view]])

(defn new-password-field-view []
  (let [field-id :gateway.user-auth/new-password
        value @(subscribe [:gateway.user-auth/field-value field-id])
        status @(subscribe [:gateway.user-auth/field-status field-id])
        errors @(subscribe [:gateway.user-auth/field-errors field-id])]
    [:div.option.password
     {:class (name status)}
     [:h2 "Password"]
     [:label
      [:div.field
       [:input {:type "password"
                :placeholder "•••••••••••"
                :value value
                :on-blur (fn [_]
                           (dispatch [:gateway.user-auth/blur field-id]))
                :on-change (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:gateway.user-auth/update-value field-id value])))}]]]
     (when (= :invalid status)
       [:div.error-message (first errors)])
     [:p "At least 8 characters. More is better!"]]))

(defn new-email-field-view []
  (let [field-id :gateway.user-auth/email
        value @(subscribe [:gateway.user-auth/field-value field-id])
        status @(subscribe [:gateway.user-auth/field-status field-id])
        errors @(subscribe [:gateway.user-auth/field-errors field-id])]
    [:div.option.email
     {:class (name status)}
     [:h2 "Email"]
     [:label
      [:div.field
       [:input {:type "email"
                :placeholder "you@awesome.com"
                :auto-complete true
                :auto-correct "off"
                :auto-capitalize "off"
                :spell-check "false"
                :auto-focus true
                :value value
                :on-blur (fn [_]
                           (dispatch [:gateway.user-auth/blur field-id]))
                :on-change (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:gateway.user-auth/update-value field-id value])))}]]
      (when (= :invalid status)
        [:div.error-message (first errors)])
      ; TODO
      #_[:p "Or, register with: "
         [auth-providers-view]]]]))

(defn register-button-view []
  (let [fields-valid?
        @(subscribe [:gateway.user-auth/fields-valid?
                     [:gateway.user-auth/email
                      :gateway.user-auth/new-password]])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     "Create a Braid Account"]))

(defn new-user-view []
  [:form.new-user
   {:no-validate true
    :on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:gateway.user-auth/submit-form
                 {:validate-fields [:gateway.user-auth/email
                                    :gateway.user-auth/new-password]
                  :dispatch-when-valid [:gateway.user-auth/remote-register]}]))}
   [:h1 "Create a Braid Account"]
   [:p "Already have one?"
    [:button
     {:type "button"
      :on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:gateway.user-auth/set-mode :log-in]))}
     "Log In"]]
   [new-email-field-view]
   [new-password-field-view]
   [register-button-view]
   [error-view]])

(defn authed-user-view []
  (let [user @(subscribe [:gateway.user-auth/user])]
    [:div.authed-user
     [:div.profile
      [:img.avatar {:src (user :avatar)}]
      [:div.info
       [:div.nickname "@" (user :nickname)]
       [:div.email (user :email)]]]
     [:p "Not you?"
      [:button
       {:type "button"
        :on-click (fn []
                    (dispatch [:gateway.user-auth/switch-account]))}
       "Sign in with a different account"]]]))

(defn checking-user-view []
  [:div.checking
   [:span "Authenticating..."]])

(defn oauth-in-progress-view []
  [:div.authorizing
   [:span "Authenticating with " (string/capitalize (name @(subscribe [:gateway.user-auth/oauth-provider]))) "..."]])

(defn user-auth-view []
  [:div.section.user-auth
   (case @(subscribe [:gateway.user-auth/user-auth-mode])
     :checking [checking-user-view]
     :register [new-user-view]
     :request-password-reset [request-password-reset-view]
     :log-in [returning-user-view]
     :authed [authed-user-view]
     :oauth-in-progress [oauth-in-progress-view])])
