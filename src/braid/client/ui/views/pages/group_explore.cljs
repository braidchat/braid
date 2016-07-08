(ns braid.client.ui.views.pages.group-explore
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.state :refer [subscribe]])
  (:import [goog.events KeyCodes]))

(defn invitations-view
  []
  (let [invitations (subscribe [:invitations])]
    (fn []
        [:div
         [:h2 "Invitations"]
         (if (seq @invitations)
           [:ul.invitations
            (for [invite @invitations]
              ^{:key (invite :id)}
              [:li.invite
               "Group "
               [:strong (invite :group-name)]
               " from "
               [:strong (invite :inviter-email)]
               [:br]
               [:button {:on-click
                         (fn [_]
                           (dispatch! :accept-invite invite))}
                "Accept"]
               [:button {:on-click
                         (fn [_]
                           (dispatch! :decline-invite invite))}
                "Decline"]])]
           [:div "No invitations."])])))

(defn create-group-view []
  (let [new-group-name (r/atom "")]
    (fn []
      [:div
       [:h2 "Create a Group"]
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
        "Create"]])))

(defn public-groups-view []
  [:div
   [:h2 "Public Groups"]
   [:div "Coming soon."]])

(defn group-explore-page-view
  []
  [:div.page.group-explore
   [:div.title "Group Explore"]
   [:div.content
    [create-group-view]
    [invitations-view]
    [public-groups-view]]])
