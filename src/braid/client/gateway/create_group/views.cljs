(ns braid.client.gateway.create-group.views
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]])
  (:import
    [goog.events KeyCodes]))

(defn group-name-field-view []
  (let [field-id :gateway.action.create-group/group-name
        value (subscribe [:gateway/field-value field-id])
        status (subscribe [:gateway/field-status field-id])
        errors (subscribe [:gateway/field-errors field-id])]
    (fn []
      [:div.option.group-name
       {:class (name @status)}
       [:label
        [:h2 "Group Name"]
        [:div.field
         [:input {:type "text"
                  :placeholder "Team Awesome"
                  :value @value
                  :on-blur (fn [_]
                             (dispatch [:gateway/blur field-id]))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:gateway/update-value field-id value])))}]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:div.explanation
         [:p "Your group's name will show up in menus and headings."]
         [:p "It doesn't need to be formal and can always be changed later."]]]])))

(defn group-url-field-view []
  (let [field-id :gateway.action.create-group/group-url
        value (subscribe [:gateway/field-value field-id])
        status (subscribe [:gateway/field-status field-id])
        errors (subscribe [:gateway/field-errors field-id])]
    (fn []
      [:div.option.group-url
       {:class (name @status)}
       [:label
        [:h2 "Group URL"]
        [:div.field
         [:input {:type "text"
                  :placeholder "awesome"
                  :autocomplete false
                  :autocorrect false
                  :autocapitalize false
                  :spellcheck false
                  :value @value
                  :on-focus (fn [_]
                              (dispatch [:gateway.action.create-group/guess-group-url]))
                  :on-blur (fn [_]
                             (dispatch [:gateway/blur field-id]))
                  :on-key-down (fn [e]
                                 (when (and
                                         (not (contains? #{KeyCodes.LEFT KeyCodes.RIGHT
                                                           KeyCodes.UP KeyCodes.DOWN
                                                           KeyCodes.TAB KeyCodes.BACKSPACE}
                                                         (.. e -keyCode)))
                                         (not (re-matches #"[A-Za-z0-9-]" (.. e -key))))
                                   (.preventDefault e)))
                  :on-change (fn [e]
                               (let [value (string/lower-case (.. e -target -value))]
                                 (dispatch [:gateway/update-value field-id value])))}]
         [:span ".braid.chat"]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:div.explanation
         [:p "Pick something short and recognizeable."]
         [:p "Lowercase letters, numbers and dashes only."]]]])))

(defn group-type-field-view []
  (let [field-id :gateway.action.create-group/group-type
        value (subscribe [:gateway/field-value field-id])
        status (subscribe [:gateway/field-status field-id])
        errors (subscribe [:gateway/field-errors field-id])]
    (fn []
      [:div.option.group-type
       {:class (name @status)}
       [:h2 "Group Type"]
       (when (= :invalid @status)
         [:div.error-message (first @errors)])
       [:label {:class (when (= "public" @value) "checked")}
        [:input {:type "radio"
                 :name "type"
                 :value "public"
                 :checked (when (= "public" @value))
                 :on-blur (fn [_]
                            (dispatch [:gateway/blur field-id]))
                 :on-click (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:gateway/update-value field-id value])))}]
        [:span "Public Group"]
        [:div.explanation
         [:p "Anyone can find your group through the Braid Group Directory."]
         [:p "Unlimited everything. Free forever."]]]
       [:label {:class (when (= "private" @value) "checked")}
        [:input {:type "radio"
                 :name "type"
                 :value "private"
                 :checked (when (= "private" @value))
                 :on-blur (fn [_]
                            (dispatch [:gateway/blur :type]))
                 :on-click (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:gateway/update-value field-id value])))}]
        [:span "Private Group"]
        [:div.explanation
         [:p "Invite-only and hidden from the Braid Group Directory."]
         [:p "Free to evaluate, then pay-what-you-want."]]]])))

(defn button-view []
  (let [fields-valid? (subscribe [:gateway/fields-valid?
                                  [:gateway.action.create-group/group-name
                                   :gateway.action.create-group/group-url
                                   :gateway.action.create-group/group-type]])
        sending? (subscribe [:gateway.action.create-group/sending?])]
    (fn []
      [:button.submit
       {:class (str (when (not @fields-valid?) "disabled") " "
                    (when @sending? "sending"))}
       "Create your Braid group"])))

(defn error-view []
  (let [error (subscribe [:gateway.action.create-group/error])]
    (fn []
      (when @error
        [:div.error-message
         "An error occured. Please try again."]))))

(defn create-group-view []
  [:div.section
   [:form
    {:on-submit (fn [e]
                  (.preventDefault e)
                  (dispatch [:gateway/submit-form
                             {:validate-fields
                              [:gateway.action.create-group/group-name
                               :gateway.action.create-group/group-url
                               :gateway.action.create-group/group-type]
                              :dispatch-when-valid [:gateway.action.create-group/remote-create-group]}]))}
    [:h1 "Start a New Braid Group"]
    [group-name-field-view]
    [group-url-field-view]
    [group-type-field-view]
    [button-view]
    [error-view]]])
