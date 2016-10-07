(ns braid.client.register.views
  (:require
    [reagent.core :as r]
    [garden.core :refer [css]]
    [garden.stylesheet :refer [at-import]]
    [clojure.string :as string])
  (:import
    [goog.events KeyCodes]))

(def braid-color "#2bb8ba")

(def small-spacing "0.5rem")
(def border-radius "3px")

(def invalid-color "#fd4734")
(def valid-color "#2bb8ba")

(defn input-field-mixin []
  {:font-size "1.25rem"
   :font-family "Open Sans"
   :padding "0.4em"
   :border "1px solid #ddd"
   :line-height 1.5
   :border-radius border-radius})

(defn form-styles []
  [:form.register
   {:display "flex"
    :flex-direction "column"
    :height "100vh"
    :padding [["3rem" 0 "1.5rem"]]
    :box-sizing "border-box"
    :justify-content "space-around"}

   [:h1
    {:margin 0
     :color braid-color
     :font-weight "normal"
     :font-size "1.75em"
     :margin-bottom "0.75rem"}

    [:&:before
     {:content "\"\""
      :display "inline-block"
      :margin-right "0.5em"
      :margin-bottom "-0.15em"
      :margin-left "-1.75em"
      :width "1.25em"
      :height "1.25em"
      :background-image "url(/images/braid-logo-color.svg)"
      :background-size "contain"}]]

   [:.option
    {:margin [[small-spacing 0]]
     :position "relative"}

    [:h2
     {:font-size "1em"
      :margin [[0 0 small-spacing 0]]}]

    [:.explanation
     {:color "#999"
      :font-size "0.75em"
      :margin [[small-spacing 0 0 0]]}

     [:p
      {:margin "0"}]]

    [:.error-message
     {:position "absolute"
      :top 0
      :right 0
      :text-align "right"
      :line-height "1.5rem"
      :font-size "0.75em"
      :color invalid-color}]

    [:label
     {:display "block"}

     ["input[type=text]"
      (input-field-mixin)

      [:&:focus
       {:border-color braid-color
        :outline "none"}]]

     ["::-webkit-input-placeholder"
      {:color "#eee"}]]

    [:&.invalid
     ["input[type=text]"
      {:border-color invalid-color}]

     [:.field::after
      (input-field-mixin)
      {:content "\"\u26a0\""
       :color invalid-color
       :border "none"
       :display "inline-block"}]]

    [:&.valid
     [:.field::after
      (input-field-mixin)
      {:content "\"\u2713\""
       :color valid-color
       :border "none"
       :display "inline-block"}]]

    [:&.group-url

     [:.field
      {:white-space "nowrap"}

      ["input[type=text]"
       {:text-align "right"
        :border-radius [[border-radius 0 0 border-radius]]
        :width "7.5em"
        :vertical-align "top" }]

      [:span
       (input-field-mixin)
       {:border-left "none"
        :display "inline-block"
        :vertical-align "top"
        :background "#f6f6f6"
        :color "#999"
        :border-radius [[0 border-radius border-radius 0]]}

       [:&::after
        {:content "\"\""
         :width "0.15em"
         :display "inline-block"}]]]]

    [:&.group-type

     [:label
      {:margin [[small-spacing 0]]
       :border "1px solid #eee"
       :padding [["0.75rem" "1rem" "1.0rem"]]
       :border-radius border-radius
       :position "relative"}

      [:&.checked
       [:&::after
        (input-field-mixin)
        {:content "\"\u2713\""
         :color valid-color
         :position "absolute"
         :right "-2em"
         :top "50%"
         :margin-top "-1em"
         :border "none"
         :display "inline-block"}]]

      [:span
       {:display "inline-block"
        :vertical-align "middle"
        :margin-left "0.35rem"}]

      [:.explanation
       {:margin-left "1.5rem"
        :margin-top "0.25em"}

       [:p
        {:display "inline"
         :margin-right "0.25em"}]]]]]

   [:button
    {:font-size "1.25em"
     :padding "1rem"
     :background braid-color
     :border "none"
     :color "white"
     :border-radius border-radius
     :text-transform "uppercase"
     :white-space "nowrap"
     :letter-spacing "0.05em"
     :display "inline-block"
     :transition "background 0.25s ease-in-out"}

    ["&[disabled]"
     {:background "#ccc"}]

    [:&::after
     {:content "\" ▶\""}]

    [:&:focus
     {:outline "none"}]]])

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

          [:html
           {:background "#f3f3f3"}]

          [:body
           {:height "100vh"
            :font-family "Open Sans"
            :max-width "23em"
            :margin "0 auto"
            :line-height 1.5
            :background "white"
            :padding [[0 "5rem"]]}]

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
                                 :message "Your group needs a name"}]
                         :url [{:valid? (not (blank? :url))
                                :message "Your group needs a URL"}
                               {:valid? (boolean (re-matches #"[a-z0-9-]*" (@fields :url)))
                                :message "Your URL can only contain lowercase letters, numbers or dashes"}
                               {:valid? (not (re-matches #"-.*" (@fields :url)))
                                :message "Your URL can't start with a -"}
                               {:valid? (not (re-matches #".*-" (@fields :url)))
                                :message "Your URL can't end with a -"}]
                         :type [{:valid? (not (blank? :type))
                                 :message "You need to select a group type"}]}
            tee (fn [x]
                  (println x) x)
            valid? (fn [field]
                     (->> (validations field)
                          (map :valid?)
                          tee
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
            [:p "Your group's name will show up in menus and headings. It doesn't need to be formal."]]]]

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
