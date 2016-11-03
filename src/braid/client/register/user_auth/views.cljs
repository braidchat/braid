(ns braid.client.register.user-auth.views
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]))

(defn auth-providers-view []
  [:span.auth-providers
   (for [provider [:github :google :facebook]]
     [:button
      {:class (name provider)
       :on-click (fn [e]
                   (.preventDefault e)
                   (dispatch [:register.user/remote-oauth provider]))}
      (string/capitalize (name provider))])])

(defn returning-email-field-view []
  [:div.option.email
   [:h2 "Email"]
   [:label
    [:div.field
     [:input {:type "email"
              :placeholder "you@awesome.com"
              :autocomplete true
              :autocorrect false
              :autocapitalize false
              :spellcheck false
              :auto-focus true}]]
    [:p "Or, log in with: "
     [auth-providers-view]]]])

(defn password-field-view []
  [:div.option.password
   [:h2 "Password"]
   [:label
    [:div.field
     [:input {:type "password"
              :placeholder "•••••••••••"}]]]
   [:p "Don't remember?"
    [:button "Reset your password"]]])

(defn returning-user-view []
  [:form.returning-user
   {:on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:register.user/remote-log-in]))}
   [:h1 "Log in to Braid"]
   [:p "Don't have an account?"
    [:button
     {:on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:register.user/set-user-register? true]))}
     "Register"]]
   [returning-email-field-view]
   [password-field-view]
   [:button.submit
    "Log in to Braid"]])

(defn new-password-field-view []
  [:div.option.password
   [:h2 "Password"]
   [:label
    [:div.field
     [:input {:type "password"
              :placeholder "•••••••••••"}]]]
   [:p "At least 8 characters. More is better!"]])

(defn new-email-field-view []
  (let [value (subscribe [:register/field-value :email])
        status (subscribe [:register/field-status :email])
        errors (subscribe [:register/field-errors :email])]
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
                             (dispatch [:blur :email]))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:update-value :email value])))}]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:p "Or, register with: "
         [auth-providers-view]]]])))

(defn new-user-view []
  [:form.new-user
   {:on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:register.user/remote-register]))}
   [:h1 "Create a Braid Account"]
   [:p "Already have one?"
    [:button
     {:on-click (fn [e]
                  (.preventDefault e)
                  (dispatch [:register.user/set-user-register? false]))}
     "Log In"]]
   [new-email-field-view]
   [new-password-field-view]
   [:button.submit
    "Create a Braid Account"]])

(defn authed-user-view []
  (let [user (subscribe [:register.user/user])]
    (fn []
      [:div.authed-user
       [:div.profile
        [:img.avatar {:src (@user :avatar)}]
        [:div.info
         [:div.nickname "@" (@user :nickname)]
         [:div.email (@user :email)]]]
       [:p "Not you?"
        [:button {:on-click (fn []
                              (dispatch [:register.user/switch-account]))}
         "Sign in with a different account"]]])))

(defn checking-user-view []
  [:div.checking
   [:span "Authenticating..."]])

(defn oauth-in-progress-view []
  (let [provider (subscribe [:register.user/oauth-provider])]
    [:div.authorizing
     [:span "Authenticating with " (string/capitalize (name @provider)) "..."]]))

(defn user-auth-view []
  (let [mode (subscribe [:register.user/user-auth-section-mode])]
    (fn []
      [:div.section.user-auth
       (case @mode
         :checking [checking-user-view]
         :register [new-user-view]
         :log-in [returning-user-view]
         :authed [authed-user-view]
         :oauth-in-progress [oauth-in-progress-view])])))
