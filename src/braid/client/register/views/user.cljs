(ns braid.client.register.views.user
  (:require
    [re-frame.core :refer [dispatch subscribe]]))

(defn email-field-view []
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
         [:p "You will use your email to sign in."]
         [:p "Double check to make sure it's correct."]]]])))


(defn auth-providers-view []
  [:div
   [:div "Github"]
   [:div "Google"]
   [:div "Facebook"]
   [:div "Twitter"]
   [:div "Microsoft"]])

(defn password-field-view []
  [:div.option.password
   [:h2 "Your Password"]
   [:label
    [:div.field
     [:input {:type "password"}]]]])

(defn user-auth-view []
  [:div
   [email-field-view]
   (when true
     [password-field-view])
   (when true
     [auth-providers-view])])
