(ns braid.client.register.views
  (:require
    [reagent.core :as r]
    [braid.client.register.styles :refer [app-styles form-styles]]
    [garden.core :refer [css]]
    [garden.stylesheet :refer [at-import]]
    [clojure.string :as string])
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
          (at-import "https://fonts.googleapis.com/css?family=Open+Sans:400,300,400italic,700")
          (app-styles)
          (form-styles))}}])

(defn header-view []
  [:h1 "Braid"])

(defn form-view []
  (let [fields (r/atom {:name ""
                        :url ""
                        :type ""})
        focused? (r/atom {:name nil
                          :url nil
                          :type nil})]
    (fn []
      (let [blank? {:name (string/blank? (@fields :name))
                    :url (string/blank? (@fields :url))
                    :type (string/blank? (@fields :type))}
            validations {:name [{:valid? (not (blank? :name))
                                 :message "Your group needs a name."}]
                         :url [{:valid? (not (blank? :url))
                                :message "Your group needs a URL."}
                               {:valid? (boolean (re-matches #"[a-z0-9-]*" (@fields :url)))
                                :message "Your URL can only contain lowercase letters, numbers or dashes."}
                               {:valid? (not (re-matches #"-.*" (@fields :url)))
                                :message "Your URL can't start with a dash."}
                               {:valid? (not (re-matches #".*-" (@fields :url)))
                                :message "Your URL can't end with a dash."}]
                         :type [{:valid? (not (blank? :type))
                                 :message "You need to select a group type"}]}
            valid? (fn [field]
                     (->> (validations field)
                          (map :valid?)
                          (every? true?)))
            messages (fn [field]
                       (->> (validations field)
                            (remove :valid?)
                            (map :message)))
            _ (println (valid? :url))
            show-status? {:name (and
                                  (not (@focused? :name))
                                  (not (blank? :name)))
                          :url (and
                                 (not (@focused? :url))
                                 (not (blank? :url)))
                          :type (and
                                  (not (@focused? :type))
                                  (not (blank? :type)))}
            all-valid? (and (valid? :name) (valid? :url) (valid? :type))]
        [:form.register
         [header-view]

         [:div.option.group-name
          {:class (when (show-status? :name)
                    (if (valid? :name)
                      "valid" "invalid"))}
          [:label
           [:h2 "Group Name"]
           [:div.field
            [:input {:type "text"
                     :placeholder "Team Awesome"
                     :auto-focus true
                     :value (@fields :name) ; TODO guess from email
                     :on-focus (fn [_]
                                 (swap! focused? assoc :name true))
                     :on-blur (fn [_]
                                (swap! focused? assoc :name false))
                     :on-change (fn [e]
                                  (let [value (.. e -target -value)]
                                    (swap! fields assoc :name value)))}]]
           (when (and (show-status? :name) (not (valid? :name)))
             [:div.error-message (first (messages :name))])
           [:div.explanation
            [:p "Your group's name will show up in menus and headings."]
            [:p "It doesn't need to be formal and can always be changed later."]]]]

         [:div.option.group-url
          {:class (when (show-status? :url)
                    (if (valid? :url)
                      "valid" "invalid"))}
          [:label
           [:h2 "Group URL"]
           [:div.field
            [:input {:type "text"
                     :placeholder "awesome"
                     :autocomplete false
                     :autocorrect false
                     :autocapitalize false
                     :spellcheck false
                     :value (@fields :url) ; TODO guess from email
                     :on-focus (fn [_]
                                 (swap! focused? assoc :url true))
                     :on-blur (fn [_]
                                (swap! focused? assoc :url false))
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
                                    (swap! fields assoc :url value)))}]
            [:span ".braid.chat"]]
           (when (and (show-status? :url) (not (valid? :url)))
             [:div.error-message (first (messages :url))])
           [:div.explanation
            [:p "Pick something short and recognizeable."]
            [:p "Lowercase letters, numbers and dashes only."]]]]

         [:div.option.group-type
          [:h2 "Group Type"]
          [:label {:class (when (= "public" (@fields :type)) "checked")}
           [:input {:type "radio"
                    :name "type"
                    :value "public"
                    :checked (when (= "public" (@fields :type)))
                    :on-focus (fn [_]
                                (swap! focused? assoc :type true))
                    :on-blur (fn [_]
                               (swap! focused? assoc :type false))
                    :on-click (fn [e]
                                (let [value (.. e -target -value)]
                                  (swap! fields assoc :type value)))}]
           [:span "Public Group"]
           [:div.explanation
            [:p "Anyone can find and join your group through the Braid Group Directory."]
            [:p "Unlimited everything. Free forever."]]]
          [:label {:class (when (= "private" (@fields :type)) "checked")}
           [:input {:type "radio"
                    :name "type"
                    :value "private"
                    :checked (when (= "private" (@fields :type)))
                    :on-focus (fn [_]
                                (swap! focused? assoc :type true))
                    :on-blur (fn [_]
                               (swap! focused? assoc :type false))
                    :on-click (fn [e]
                                (let [value (.. e -target -value)]
                                  (swap! fields assoc :type value)))}]
           [:span "Private Group"]
           [:div.explanation
            [:p "Invite-only and hidden from the Braid Group Directory."]
            [:p "Free to evaluate, then pay-what-you-want."]]]]

         [:button {:disabled (not all-valid?)}
          "Create your group"]]))))

(defn app-view []
  [:div.app
   [style-view]
   [form-view]])
