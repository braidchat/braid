(ns braid.client.register.views
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [braid.client.register.styles :refer [app-styles form-styles]]
    [garden.core :refer [css]]
    [garden.stylesheet :refer [at-import]])
  (:import
    [goog.events KeyCodes]))

(defn style-view []
  [:style
   {:type "text/css"
    :dangerouslySetInnerHTML
    {:__html
     (css {:auto-prefix #{:transition
                          :flex-direction
                          :flex-shrink
                          :align-items
                          :animation
                          :flex-grow}
           :vendors ["webkit"]}
          (at-import "https://fonts.googleapis.com/css?family=Open+Sans:400,700")
          (app-styles)
          (form-styles))}}])

(defn header-view []
  [:h1 "Braid"])

(defn group-name-field-view []
  (let [value (subscribe [:register/field-value :name])
        status (subscribe [:register/field-status :name])
        errors (subscribe [:register/field-errors :name])]
    (fn []
      [:div.option.group-name
       {:class (name @status)}
       [:label
        [:h2 "Group Name"]
        [:div.field
         [:input {:type "text"
                  :placeholder "Team Awesome"
                  :auto-focus true
                  :value @value
                  :on-blur (fn [_]
                             (dispatch [:blur :name]))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:update-value :name value])))}]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:div.explanation
         [:p "Your group's name will show up in menus and headings."]
         [:p "It doesn't need to be formal and can always be changed later."]]]])))

(defn group-url-field-view []
  (let [value (subscribe [:register/field-value :url])
        status (subscribe [:register/field-status :url])
        errors (subscribe [:register/field-errors :url])]
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
                              (dispatch [:guess-group-url]))
                  :on-blur (fn [_]
                             (dispatch [:blur :url]))
                  :on-key-down (fn [e]
                                 (when (and
                                         (not (contains? #{KeyCodes.LEFT KeyCodes.RIGHT
                                                           KeyCodes.UP KeyCodes.DOWN
                                                           KeyCodes.TAB KeyCodes.BACKSPACE}
                                                         (.. e -keyCode)))
                                         (not (re-matches #"[a-z0-9-]" (.. e -key))))
                                   (.preventDefault e)))
                  :on-change (fn [e]
                               (let [value (.. e -target -value)]
                                 (dispatch [:update-value :url value])))}]
         [:span ".braid.chat"]]
        (when (= :invalid @status)
          [:div.error-message (first @errors)])
        [:div.explanation
         [:p "Pick something short and recognizeable."]
         [:p "Lowercase letters, numbers and dashes only."]]]])))

(defn group-type-field-view []
  (let [value (subscribe [:register/field-value :type])
        status (subscribe [:register/field-status :type])
        errors (subscribe [:register/field-errors :type])]
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
                            (dispatch [:blur :type]))
                 :on-click (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:update-value :type value])))}]
        [:span "Public Group"]
        [:div.explanation
         [:p "Anyone can find and join your group through the Braid Group Directory."]
         [:p "Unlimited everything. Free forever."]]]
       [:label {:class (when (= "private" @value) "checked")}
        [:input {:type "radio"
                 :name "type"
                 :value "private"
                 :checked (when (= "private" @value))
                 :on-blur (fn [_]
                            (dispatch [:blur :type]))
                 :on-click (fn [e]
                             (let [value (.. e -target -value)]
                               (dispatch [:update-value :type value])))}]
        [:span "Private Group"]
        [:div.explanation
         [:p "Invite-only and hidden from the Braid Group Directory."]
         [:p "Free to evaluate, then pay-what-you-want."]]]])))

(defn button-view []
  (let [name-valid? (subscribe [:register/field-valid? :name])
        url-valid? (subscribe [:register/field-valid? :url])
        type-valid? (subscribe [:register/field-valid? :type])]
    (fn []
      (let [all-valid? (and @name-valid? @url-valid? @type-valid?)]
        [:button {:class (when (not all-valid?) "disabled")
                  :on-click (fn [e]
                              (.preventDefault e)
                              (dispatch [:submit-form]))}
         "Create your group"]))))

(defn form-view []
  [:form.register
   [header-view]
   [group-name-field-view]
   [group-url-field-view]
   [group-type-field-view]
   [button-view]])

(defn app-view []
  [:div.app
   [style-view]
   [form-view]])
