(ns braid.client.ui.views.pages.group-explore
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]])
  (:import
    [goog.events KeyCodes]))

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
                           (dispatch [:accept-invite invite]))}
                "Accept"]
               [:button {:on-click
                         (fn [_]
                           (dispatch [:decline-invite invite]))}
                "Decline"]])]
           [:div "No invitations."])])))

(defn public-groups-view []
  [:div
   [:h2 "Public Groups"]
   [:div "Coming soon."]])

(defn group-explore-page-view
  []
  [:div.page.group-explore
   [:div.title "Group Explore"]
   [:div.content
    [:a {:href "/gateway/create-group"} "Create a Group"]
    [invitations-view]
    [public-groups-view]]])
