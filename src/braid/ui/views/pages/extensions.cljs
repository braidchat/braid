(ns braid.ui.views.pages.extensions
  (:require [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]))

; NOTE THIS PAGE IS UNTESTED SINCE REAGENT CONVERSION

(defn new-asana-form
  []
  [:form
   [:input {:type "text" :placeholder "foobar"}]])

; TODO: how to get list of available extensions?
(def available-extensions
  {"asana" new-asana-form})

(defn new-extension-view
  []
  (let [type (r/atom nil)
        set-type! (fn [value]
                    (reset! type value))]
    (fn []
      [:div.new-extension
       [:pre (pr-str type)]

       [:select.extension-select
        {:on-change (fn [e]
                      (set-type! (.. e -target -value)))}
        (for [extension (cons nil (keys available-extensions))]
          [:option {:value extension}
           ((fnil name "") extension)])]

       (get available-extensions type (constantly nil))])))

(defn extensions-page-view
  []
  (let [; TODO create this subscription
        extensions (subscribe [:open-group-extensions])]
    (fn []
      [:div.page.extensions
       [:h1 "Extensions"]

       [:div.extensions-list
        (for [extension @extensions]
          [:div.extensions
           (str (extension :id) ": " (name (extension :type)))])]

       [new-extension-view]])))
