(ns braid.client.gateway.user-auth.views
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]))

(defn auth-providers-view []
  [:span.auth-providers
   (for [provider [:github :google :facebook]]
     ^{:key provider}
     [:button
      {:class (name provider)
       :on-click (fn [e]
                   (.preventDefault e)
                   (dispatch [:gateway.user-auth/remote-oauth provider]))}
      (string/capitalize (name provider))])])

(defn returning-email-field-view []
  (let [field-id :gateway.user-auth/email
        value (subscribe [:gateway/field-value field-id])
        status (subscribe [:gateway/field-status field-id])
        errors (subscribe [:gateway/field-errors field-id])]
    (fn []
      [:div.option.email
       {:class (name @status)}
       [:h2 "Email"]
       [:label
        [:div.field
         [:input {:type "email"
                  :placeholder "you@awesome.com"
                  :autocomplete true
                  :autocorrect false
                  :autocapitalize false
                  :spellcheck false
                  :auto-focus true
                  :value @value
                  :on-blur (fn [_]
                             (dispatch [:gateway/blur field-id]))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:gateway/update-value field-id value])))
                  }]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:p "Or, log in with: "
         [auth-providers-view]]]])))

(defn returning-password-field-view []
  (let [field-id :gateway.user-auth/password
        value (subscribe [:gateway/field-value field-id])
        status (subscribe [:gateway/field-status field-id])
        errors (subscribe [:gateway/field-errors field-id])]
    (fn []
      [:div.option.password
       {:class (name @status)}
       [:h2 "Password"]
       [:label
        [:div.field
         [:input {:type "password"
                  :placeholder "•••••••••••"
                  :value @value
                  :on-blur (fn [_]
                             (dispatch [:gateway/blur field-id]))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:gateway/update-value field-id value])))}]]]
       (when (= :invalid @status)
         [:div.error-message (first @errors)])
       [:p "Don't remember?"
        [:button
         {:on-click (fn [])}
         "Reset your password"]]])))

(defn login-button-view []
  (let [fields-valid?
        (subscribe [:gateway/fields-valid?
                    [:gateway.user-auth/email
                     :gateway.user-auth/password]])]
    (fn []
      [:button.submit
       {:class (when-not @fields-valid? "disabled")}
       "Log in to Braid"])))

(defn error-view []
  (let [error (subscribe [:gateway.user-auth/error])]
    (fn []
      (when @error
        (case @error
          :email-exists
          [:div.error-message
           "A user is already registered with that email."
           [:button
            {:on-click (fn [e]
                         (.preventDefault e)
                         (dispatch [:gateway.user-auth/clear-error])
                         (dispatch [:gateway.user-auth/set-user-register? false]))}
            "Log In"]]

          ; catch-all
          [:div.error-message
           "An error occured. Please try again."])))))

(defn returning-user-view []
  [:form.returning-user
   {:on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:gateway/submit-form
                 {:validate-fields [:gateway.user-auth/email
                                    :gateway.user-auth/password]
                  :dispatch-when-valid [:gateway.user-auth/remote-log-in]}]))}
   [:h1 "Log in to Braid"]
   [:p "Don't have an account?"
    [:button
     {:on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:gateway.user-auth/set-user-register? true]))}
     "Register"]]
   [returning-email-field-view]
   [returning-password-field-view]
   [login-button-view]
   [error-view]])

(defn new-password-field-view []
  (let [field-id :gateway.user-auth/password
        value (subscribe [:gateway/field-value field-id])
        status (subscribe [:gateway/field-status field-id])
        errors (subscribe [:gateway/field-errors field-id])]
    (fn []
      [:div.option.password
       {:class (name @status)}
       [:h2 "Password"]
       [:label
        [:div.field
         [:input {:type "password"
                  :placeholder "•••••••••••"
                  :value @value
                  :on-blur (fn [_]
                             (dispatch [:gateway/blur field-id]))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:gateway/update-value field-id value])))}]]]
       (when (= :invalid @status)
         [:div.error-message (first @errors)])
       [:p "At least 8 characters. More is better!"]])))

(defn new-email-field-view []
  (let [field-id :gateway.user-auth/email
        value (subscribe [:gateway/field-value field-id])
        status (subscribe [:gateway/field-status field-id])
        errors (subscribe [:gateway/field-errors field-id])]
    (fn []
      [:div.option.email
       {:class (name @status)}
       [:h2 "Email"]
       [:label
        [:div.field
         [:input {:type "email"
                  :placeholder "you@awesome.com"
                  :autocomplete true
                  :autocorrect false
                  :autocapitalize false
                  :spellcheck false
                  :auto-focus true
                  :value @value
                  :on-blur (fn [_]
                             (dispatch [:gateway/blur field-id]))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:gateway/update-value field-id value])))}]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:p "Or, register with: "
         [auth-providers-view]]]])))

(defn register-button-view []
  (let [fields-valid?
        (subscribe [:gateway/fields-valid?
                    [:gateway.user-auth/email
                     :gateway.user-auth/password]])]
    (fn []
      [:button.submit
       {:class (when-not @fields-valid? "disabled")}
       "Create a Braid Account"])))

(defn new-user-view []
  [:form.new-user
   {:on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:gateway/submit-form
                 {:validate-fields [:gateway.user-auth/email
                                    :gateway.user-auth/password]
                  :dispatch-when-valid [:gateway.user-auth/remote-register]}]))}
   [:h1 "Create a Braid Account"]
   [:p "Already have one?"
    [:button
     {:on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:gateway.user-auth/set-user-register? false]))}
     "Log In"]]
   [new-email-field-view]
   [new-password-field-view]
   [register-button-view]
   [error-view]])

(defn authed-user-view []
  (let [user (subscribe [:gateway.user-auth/user])]
    (fn []
      [:div.authed-user
       [:div.profile
        [:img.avatar {:src (@user :avatar)}]
        [:div.info
         [:div.nickname "@" (@user :nickname)]
         [:div.email (@user :email)]]]
       [:p "Not you?"
        [:button {:on-click (fn []
                              (dispatch [:gateway.user-auth/switch-account]))}
         "Sign in with a different account"]]])))

(defn checking-user-view []
  [:div.checking
   [:span "Authenticating..."]])

(defn oauth-in-progress-view []
  (let [provider (subscribe [:gateway.user-auth/oauth-provider])]
    [:div.authorizing
     [:span "Authenticating with " (string/capitalize (name @provider)) "..."]]))

(defn user-auth-view []
  (let [mode (subscribe [:gateway.user-auth/user-auth-mode])]
    (fn []
      [:div.section.user-auth
       (case @mode
         :checking [checking-user-view]
         :register [new-user-view]
         :log-in [returning-user-view]
         :authed [authed-user-view]
         :oauth-in-progress [oauth-in-progress-view])])))
