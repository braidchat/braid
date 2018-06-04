(ns braid.core.client.ui.views.pages.group-explore
  (:require
   [braid.core.client.helpers :as helpers :refer [->color]]
   [braid.core.client.routes :as routes]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r])
  (:import
   (goog.events KeyCodes)))

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

(defn public-group-view
  [group]
  [:a.group {:style {:background-color (->color (:id group))}
             :href (routes/inbox-page-path {:group-id (:id group)})}
   [:div.name (:name group)]
   [:div.info
    [:div (:intro group)]
    [:div "Last Updated " (helpers/format-date (:updated-at group))]
    [:div "Created " (helpers/format-date (:created-at group))]]
   [:div.users (let [count (:users-count group)]
                 (str count " user" (when (not= 1 count) "s")))]])

(defn public-groups-view []
  (let [subscribed-groups @(subscribe [:subscribed-group-ids])]
    [:div
     [:h2 "Public Groups"]
     [:div.public-groups
      (doall
        (for [group (->> @(subscribe [:core/public-groups])
                        (sort-by :updated-at #(compare %2 %1)))
              :when (not (subscribed-groups (:id group)))]
          ^{:key (:id group)}
          [public-group-view group]))]]))

(defn group-explore-page-view
  []
  [:div.page.group-explore
   [:div.title "Group Explore"]
   [:div.content
    [:a {:href (routes/other-path {:page-id "create-group"})}
     "Create Group"]
    [invitations-view]
    [public-groups-view]]])
