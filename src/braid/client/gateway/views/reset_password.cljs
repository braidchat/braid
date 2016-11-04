(ns braid.client.gateway.views.reset-password)

(defn password-field-view []
  [:div.option.password
   [:h2 "New Password"]
   [:label
    [:div.field
     [:input {:type "password"
              :placeholder "•••••••••••"}]]]
   [:div.explanation
    [:p "At least 8 characters. More is better!"]]])

(defn reset-password-view []
  [:div
   [:h2 "Reset Your Password"]
   [password-field-view]])
