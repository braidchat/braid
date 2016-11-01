(ns braid.client.register.views.user
  (:require
    [re-frame.core :refer [dispatch subscribe]]))

(defn auth-providers-view []
  [:div.auth-providers
   [:a.github "Github"]
   [:a.google "Google"]
   [:a.facebook "Facebook"]])

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
        [:div.explanation
         [:p "Or, register with: "]
         [auth-providers-view]]]])))

(defn returning-email-field-view []
  [:div.option.email
   [:h2 "Your Email"]
   [:label
    [:div.field
     [:input {:type "email"
              :placeholder "you@awesome.com"
              :autocomplete true
              :autocorrect false
              :autocapitalize false
              :spellcheck false
              :auto-focus true}]]
    [:div.explanation
     [:p "Or, login with: "
      [auth-providers-view]]]]])

(defn returning-password-field-view []
  [:div.option.password
   [:h2 "Your Password"]
   [:label
    [:div.field
     [:input {:type "password"
              :placeholder "•••••••••••"}]]]
   [:div.explanation
    [:p "Don't remember your password?"
     [:a "Reset it."]]]])

(defn new-password-field-view []
  [:div.option.password
   [:h2 "Your Password"]
   [:label
    [:div.field
     [:input {:type "password"
              :placeholder "•••••••••••"}]]]
   [:div.explanation
    [:p "At least 8 characters. More is better!"]]])

(defn new-user-view []
  [:div
   [new-email-field-view]])

(defn returning-user-view []
  [:div
   [returning-email-field-view]
   [returning-password-field-view]])

(defn user-auth-view []
  [:div
   (if false
     [new-user-view]
     [returning-user-view])])
