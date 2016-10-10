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

(defn ajax-request [config]
  ((config :handler) true))

(defn form-view []
  (let [fields (r/atom {:name {:value ""
                               :focused? nil
                               :loading 0
                               :errors []}
                        :url {:value ""
                              :focused? nil
                              :loading 0
                              :errors []}
                        :type {:value ""
                               :focused? nil
                               :loading 0
                               :errors []}})]
    (fn []
      (let [blank? {:name (string/blank? (get-in @fields [:name :value]))
                    :url (string/blank? (get-in @fields [:url :value]))
                    :type (string/blank? (get-in @fields [:type :value]))}
            validations {:name [(fn [name cb]
                                  (if (string/blank? name)
                                    (cb "Your group needs a name.")
                                    (cb nil)))]
                         :url [(fn [url cb]
                                 (if (string/blank? url)
                                   (cb "Your group needs a URL.")
                                   (cb nil)))
                               (fn [url cb]
                                 (if (not (re-matches #"[a-z0-9-]*" url))
                                   (cb "Your URL can only contain lowercase letters, numbers or dashes.")
                                   (cb nil)))
                               (fn [url cb]
                                 (if (re-matches #"-.*" url)
                                   (cb "Your URL can't start with a dash.")
                                   (cb nil)))
                               (fn [url cb]
                                 (if (re-matches #".*-" url)
                                   (cb "Your URL can't end with a dash.")
                                   (cb nil)))
                               (fn [url cb]
                                 (ajax-request
                                   {:path ""
                                    :handler (fn [valid?]
                                               (if valid?
                                                 (cb nil)
                                                 (cb "Your group URL is already taken; try another.")))}))]
                         :type [(fn [type cb]
                                  (when (string/blank? type)
                                    (cb "You need to select a group type")
                                    (cb nil)))]}
            validate! (fn []
                        (doseq [[field fns] validations]
                          (swap! fields assoc-in [field :errors] [])
                          (swap! fields assoc-in [field :loading] (count fns))
                          (doseq [validator-fn fns]
                            (validator-fn
                              (get-in @fields [field :value])
                              (fn [status]
                                (cond
                                  (= status :loading) (swap! fields update-in [field :loading] dec)
                                  (string? status) (swap! fields update-in [field :errors] conj status)
                                  (nil? status) :do-nothing))))))
            valid? (fn [field]
                     (empty? (get-in @fields [field :errors])))
            show-status? {:name (and
                                  (not (get-in @fields [:name :focused?]))
                                  (not (blank? :name)))
                          :url (and
                                 (not (get-in @fields [:url :focused?]))
                                 (not (blank? :url)))
                          :type (and
                                  (not (get-in @fields [:type :focused?]))
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
                     :value (get-in @fields [:name :value])
                     :on-focus (fn [_]
                                 (swap! fields assoc-in [:name :focused?] true))
                     :on-blur (fn [_]
                                (swap! fields assoc-in [:name :focused?] false)
                                (validate!))
                     :on-change (fn [e]
                                  (let [value (.. e -target -value)]
                                    (swap! fields assoc-in [:name :value] value)))}]]
           (when (and (show-status? :name) (not (valid? :name)))
             [:div.error-message (first (get-in @fields [:name :errors]))])
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
                     :value (get-in @fields [:url :value])
                     :on-focus (fn [_]
                                 (swap! fields assoc-in [:url :focused?] true))
                     :on-blur (fn [_]
                                (swap! fields assoc-in [:url :focused?] false)
                                (validate!))
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
                                    (swap! fields assoc-in [:url :value] value)))}]
            [:span ".braid.chat"]]
           (when (and (show-status? :url) (not (valid? :url)))
             [:div.error-message (first (get-in @fields [:url :errors]))])
           [:div.explanation
            [:p "Pick something short and recognizeable."]
            [:p "Lowercase letters, numbers and dashes only."]]]]

         [:div.option.group-type
          [:h2 "Group Type"]
          [:label {:class (when (= "public" (get-in @fields [:type :value])) "checked")}
           [:input {:type "radio"
                    :name "type"
                    :value "public"
                    :checked (when (= "public" (get-in @fields [:type :value])))
                    :on-focus (fn [_]
                                (swap! fields assoc-in [:type :focused?] true))
                    :on-blur (fn [_]
                               (swap! fields assoc-in [:type :focused?] false)
                               (validate!))
                    :on-click (fn [e]
                                (let [value (.. e -target -value)]
                                  (swap! fields assoc-in [:type :value] value)))}]
           [:span "Public Group"]
           [:div.explanation
            [:p "Anyone can find and join your group through the Braid Group Directory."]
            [:p "Unlimited everything. Free forever."]]]
          [:label {:class (when (= "private" (get-in @fields [:type :value])) "checked")}
           [:input {:type "radio"
                    :name "type"
                    :value "private"
                    :checked (when (= "private" (get-in @fields [:type :value])))
                    :on-focus (fn [_]
                                (swap! fields assoc-in [:type :focused?] true))
                    :on-blur (fn [_]
                               (swap! fields assoc-in [:type :focused?] false)
                               (validate!))
                    :on-click (fn [e]
                                (let [value (.. e -target -value)]
                                  (swap! fields assoc-in [:type :value] value)))}]
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
