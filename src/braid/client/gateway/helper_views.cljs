(ns braid.client.gateway.helper-views
  (:require
    [re-frame.core :refer [subscribe dispatch]]))

(defn field-view [opts]
  (let [field-id (opts :id)
        value @(subscribe [:gateway.action.reset-password/field-value field-id])
        status @(subscribe [:gateway.action.reset-password/field-status field-id])
        errors @(subscribe [:gateway.action.reset-password/field-errors field-id])]
    [:div.option
     {:class (str (opts :class) " " (name status))}
     [:h2 (opts :title)]
     [:label
      [:div.field
       [:input {:type (opts :type)
                :placeholder (opts :placeholder)
                :value value
                :on-blur (fn [_]
                           (dispatch [:gateway.action.reset-password/blur field-id]))
                :on-change (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:gateway.action.reset-password/update-value field-id value])))}]]
      (when (= :invalid status)
        [:div.error-message (first errors)])
      [:p.explanation (opts :help-text)]]]))

(defn button-view [opts]
  (let [fields-valid?
        @(subscribe [:gateway.user-auth/fields-valid?
                     (opts :validations)])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     (opts :text)]))

(defn form-view [opts & body]
  [:div.section
   {:class (str (opts :class) " "
                (when (opts :disabled?) "disabled"))}
   [:form
    {:no-validate true
     :on-submit (opts :on-submit)}

    (into [:fieldset {:disabled (opts :disabled?)}]
          (concat [[:h1 (opts :title)]] body))]])
