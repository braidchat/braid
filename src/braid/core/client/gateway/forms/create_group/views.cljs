(ns braid.core.client.gateway.forms.create-group.views
  (:require
   [braid.core.client.gateway.helper-views :refer [form-view field-view]]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch subscribe]])
  (:import
   (goog.events KeyCodes)))

(defn group-name-field-view []
  [field-view
   {:id :group-name
    :subs-ns :braid.core.client.gateway.forms.create-group.subs
    :disp-ns :braid.core.client.gateway.forms.create-group.events
    :title "Group Name"
    :class "group-name"
    :type "text"
    :placeholder "Team Awesome"
    :on-blur (fn [e]
               (dispatch [:braid.core.client.gateway.forms.create-group.events/guess-group-url]))
    :help-text [:div
                [:p "Your group's name will show up in menus and headings."]
                [:p "It doesn't need to be formal and can always be changed later."]]}])

(defn group-url-field-view []
  [field-view
   {:id :group-url
    :subs-ns :braid.core.client.gateway.forms.create-group.subs
    :disp-ns :braid.core.client.gateway.forms.create-group.events
    :title "Group URL"
    :class "group-url"
    :type "text"
    :placeholder "awesome"
    :pre-input [:span.domain "braid.chat∕"]
    :help-text [:div
                [:p "Pick something short and recognizeable."]
                [:p "Lowercase letters, numbers and dashes only."]]
    :on-key-down (fn [e]
                   (when (and
                           (not (contains? #{KeyCodes.LEFT KeyCodes.RIGHT
                                             KeyCodes.UP KeyCodes.DOWN
                                             KeyCodes.TAB KeyCodes.BACKSPACE}
                                           (.. e -keyCode)))
                           (not (re-matches #"[A-Za-z0-9-]" (.. e -key))))
                     (.preventDefault e)))
    :on-change-transform string/lower-case}])

(defn group-type-field-view []
  (let [field-id :group-type
        value @(subscribe [:braid.core.client.gateway.forms.create-group.subs/field-value field-id])
        status @(subscribe [:braid.core.client.gateway.forms.create-group.subs/field-status field-id])
        errors @(subscribe [:braid.core.client.gateway.forms.create-group.subs/field-errors field-id])]
    [:div.option.group-type
     {:class (name status)}
     [:h2 "Group Type"]
     (when (= :invalid status)
       [:div.error-message (first errors)])
     [:label {:class (when (= "public" value) "checked")}
      [:input {:type "radio"
               :name "type"
               :value "public"
               :checked (when (= "public" value))
               :on-blur (fn [_]
                          (dispatch [:braid.core.client.gateway.forms.create-group.events/blur field-id]))
               :on-click (fn [e]
                           (let [value (.. e -target -value)]
                             (dispatch [:braid.core.client.gateway.forms.create-group.events/update-value field-id value])))}]
      [:span "Public Group"]
      [:div.explanation
       [:p "Anyone can find your group through the Braid Group Directory."]
       [:p "Unlimited everything. Free forever."]]]
     [:label {:class (when (= "private" value) "checked")}
      [:input {:type "radio"
               :name "type"
               :value "private"
               :checked (when (= "private" value))
               :on-blur (fn [_]
                          (dispatch [:braid.core.client.gateway.forms.create-group.events/blur :type]))
               :on-click (fn [e]
                           (let [value (.. e -target -value)]
                             (dispatch [:braid.core.client.gateway.forms.create-group.events/update-value field-id value])))}]
      [:span "Private Group"]
      [:div.explanation
       [:p "Invite-only and hidden from the Braid Group Directory."]
       [:p "Free to evaluate, then pay-what-you-want."]]]]))

(defn button-view []
  (let [fields-valid? @(subscribe [:braid.core.client.gateway.forms.create-group.subs/fields-valid?
                                  [:group-name
                                   :group-url
                                   :group-type]])
        sending? @(subscribe [:braid.core.client.gateway.forms.create-group.subs/sending?])]
    [:button.submit
     {:type "submit"
      :class (str (when (not fields-valid?) "disabled") " "
                  (when sending? "sending"))}
     "Create your Braid group"]))

(defn error-view []
  (let [error @(subscribe [:braid.core.client.gateway.forms.create-group.subs/error])]
    (when error
      [:div.error-message
       "An error occured. Please try again."])))

(defn create-group-view []
  [form-view
   {:title "Start a New Braid Group"
    :class "create-group"
    :disabled? @(subscribe [:braid.core.client.gateway.subs/action-disabled?])
    :on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:braid.core.client.gateway.forms.create-group.events/submit-form
                 {:validate-fields
                  [:group-name
                   :group-url
                   :group-type]
                  :dispatch-when-valid [:braid.core.client.gateway.forms.create-group.events/remote-create-group]}]))}
   [group-name-field-view]
   [group-url-field-view]
   [group-type-field-view]
   [button-view]
   [error-view]])
