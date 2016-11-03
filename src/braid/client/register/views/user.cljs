(ns braid.client.register.views.user
  (:require
    [re-frame.core :refer [dispatch subscribe]]))

(defn auth-providers-view []
  [:span.auth-providers
   [:button.github
    {:on-click (fn []
                 (dispatch [:register.user/remote-oauth :github]))}
    "Github"]
   [:button.google
    {:on-click (fn []
                 (dispatch [:register.user/remote-oauth :google]))}
    "Google"]
   [:button.facebook
    {:on-click (fn []
                 (dispatch [:register.user/remote-oauth :facebook]))}
    "Facebook"]])

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
    [:p "Or, login with: "
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
  [:div.returning-user
   [:h1 "Log In to Braid"]
   [:p "Don't have an account?"
    [:button {:on-click (fn [_]
                     (dispatch [:register.user/set-user-register? true]))}
     "Register"]]
   [returning-email-field-view]
   [password-field-view]])

(defn new-email-field-view []
  (let [value (subscribe [:register/field-value :email])
        status (subscribe [:register/field-status :email])
        errors (subscribe [:register/field-errors :email])]
    (fn []
      [:div.option.email
       {:class (name @status)}
       [:h2 "Your Email"]
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
                                 (dispatch [:update-value :email value]))                              )}]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:p "Or, register with: "
         [auth-providers-view]]]])))

(defn new-user-view []
  [:div.new-user
   [:h1 "Create a Braid Account"]
   [:p "Already have one?"
    [:button
     {:on-click (fn [_]
                  (dispatch [:register.user/set-user-register? false]))}
     "Log In"]]
   [new-email-field-view]])

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
  [:div
   "Checking..."])

(defn oauth-in-progress-view []
  [:div
   "Authorizing..."])

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
