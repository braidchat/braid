(ns braid.client.gateway.forms.join-group.views
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]
    [braid.client.gateway.helper-views :refer [form-view field-view]]))

(defn error-view []
  (let [error @(subscribe [:braid.client.gateway.forms.join-group.subs/error])]
    (when error
      [:div.error-message
       "An error occured. Please try again."])))

(defn button-view []
  (let [sending? @(subscribe [:braid.client.gateway.forms.join-group.subs/sending?])
        group @(subscribe [:braid.client.gateway.forms.join-group.subs/group])]
    [:button.submit
     {:type "submit"
      :class (str (when sending? "sending"))}
     "Join " (group :name)]))

(defn group-info-view [group]
  [:div.group-info
   (when (group :avatar)
     [:img {:src (group :avatar)}])
   [:h1 "Join " [:span.name (group :name)] " on Braid"]
   (when (group :intro)
     [:p.intro (group :intro)])
   (when (group :users-count)
     [:p.members-count (group :users-count) " members"])])

(defn join-group-view []
  (let [group @(subscribe [:braid.client.gateway.forms.join-group.subs/group])]
    (when group
      (cond
        (and (group :id) (group :public?))
        [form-view
         {:class "join-group"
          :disabled? @(subscribe [:gateway/action-disabled?])
          :on-submit
          (fn [e]
            (.preventDefault e)
            (dispatch [:braid.client.gateway.forms.join-group.events/submit-form]))}
         [group-info-view group]
         [button-view]
         [error-view]]

        (and (group :id) (not (group :public?)))
        [:div.section.join-group
         [:form
          [:fieldset
           [group-info-view group]
           [:div.error-message "This is not a public group. Please contact a group admin to receive an invite."]]]]

        :else
        [:div.section.join-group
         [:form
          [:fieldset
           [:div.error-message "A group with this id does not exist."]]]]))))
