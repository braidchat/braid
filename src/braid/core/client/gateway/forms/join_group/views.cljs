(ns braid.core.client.gateway.forms.join-group.views
  (:require
   [braid.core.client.gateway.helper-views :refer [form-view]]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch subscribe]]))

(defn error-view []
  (let [error @(subscribe [:braid.core.client.gateway.forms.join-group.subs/error])]
    (when error
      [:div.error-message
       "An error occured. Please try again."])))

(defn button-view []
  (let [sending? @(subscribe [:braid.core.client.gateway.forms.join-group.subs/sending?])
        group @(subscribe [:braid.core.client.gateway.forms.join-group.subs/group])]
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
  (let [group @(subscribe [:braid.core.client.gateway.forms.join-group.subs/group])]
    (when group
      (cond
        (and (group :id) (group :public?))
        [form-view
         {:class "join-group"
          :disabled? @(subscribe [:braid.core.client.gateway.subs/action-disabled?])
          :on-submit
          (fn [e]
            (.preventDefault e)
            (dispatch [:braid.core.client.gateway.forms.join-group.events/submit-form]))}
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
