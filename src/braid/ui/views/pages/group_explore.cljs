(ns braid.ui.views.pages.group-explore
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [chat.client.dispatcher :refer [dispatch!]])
  (:import [goog.events KeyCodes]))

(defn group-explore-page-view
  []
  (let [new-group-name (r/atom "")]
    (fn []
      [:div.page.group-explore
       [:div.title "Group Explore"]
       [:div.content
        [:p "One day, you'll be able to see various open groups that may be worth exploring here."]

        [:h2 "Create Group"]
        [:div.new-group
         [:label "New Group"
          [:input {:placeholder "Group Name"
                   :value @new-group-name
                   :on-change (fn [e] (reset! new-group-name (.. e -target -value)))
                   :on-keydown
                   (fn [e]
                     (when (= KeyCodes.ENTER e.keyCode)
                       (.preventDefault e)
                       (dispatch! :create-group {:name @new-group-name})
                       (reset! new-group-name "")))}]
          [:button {:disabled (string/blank? @new-group-name)
                    :on-click (fn [_]
                                (dispatch! :create-group {:name @new-group-name})
                                (reset! new-group-name ""))}
           "Create"]]]]])))
