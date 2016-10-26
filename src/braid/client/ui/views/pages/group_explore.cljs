(ns braid.client.ui.views.pages.group-explore
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [re-frame.core :refer [dispatch subscribe]])
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
                           (dispatch [:accept-invite invite]))}
                "Accept"]
               [:button {:on-click
                         (fn [_]
                           (dispatch [:decline-invite invite]))}
                "Decline"]])]
           [:div "No invitations."])])))

(defn create-group-view []
  (let [new-group-data (r/atom {:name ""
                                :slug ""})]
    (fn []
      [:div
       [:h2 "Create a Group"]
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (dispatch [:request-create-group @new-group-data])
                            (reset! new-group-data {:name ""
                                                    :slug ""}))}
        [:label "Group Name"
         [:input {:value (@new-group-data :name)
                  :on-change (fn [e]
                               (swap! new-group-data assoc :name (.. e -target -value)))}]]
        [:label "Group Domain"
         [:input {:value (@new-group-data :slug)
                  :on-change (fn [e]
                               (swap! new-group-data assoc :slug (.. e -target -value)))}]]
        [:button {:disabled
                  ; TODO validate slug format and uniqueness
                  (or (string/blank? (@new-group-data :name))
                      (string/blank? (@new-group-data :slug)))}
         "Create Group"]]])))

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
