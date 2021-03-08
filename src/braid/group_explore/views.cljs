(ns braid.group-explore.views
  (:require
   [braid.lib.color :as color]
   [braid.lib.date :as date]
   [braid.core.client.routes :as routes]
   [braid.core.hooks :as hooks]
   [cljs-time.core :as t]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r])
  (:import
   (goog.events KeyCodes)))

(defonce group-explore-links
  (hooks/register! (atom []) [{:title string?
                               :url string?}]))

(defn- invitations-view
  []
  (let [invitations (subscribe [:invitations])]
    (fn []
      [:div.invitations
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
                         (dispatch [:accept-invite! invite]))}
              "Accept"]
             [:button {:on-click
                       (fn [_]
                         (dispatch [:decline-invite! invite]))}
              "Decline"]])]
         [:div "No invitations."])])))

(defn- public-group-view
  [group]
  [:a.group {:style {:background-color (color/->color (:id group))}
             :href (routes/group-page-path {:group-id (:id group)
                                            :page-id "inbox"})}
   [:div.name (:name group)]
   [:div.info
    [:div (:intro group)]
    [:div "Last Updated " (date/smart-format-date (:updated-at group))]
    [:div "Created " (date/smart-format-date (:created-at group))]]
   [:div.users (let [count (:users-count group)]
                 (str count " user" (when (not= 1 count) "s")))]])

(defn- public-groups-view []
  (let [subscribed-groups @(subscribe [:braid.group-explore/subscribed-group-ids])
        showing? (r/atom false)]
    (fn []
      (let [groups (->> @(subscribe [:braid.group-explore/public-groups])
                        (sort-by :updated-at #(compare %2 %1)))
            cutoff (t/minus (t/now) (t/weeks 1))
            {active-groups true stale-groups false}
            (->> groups
                 (group-by (comp #(t/after? % cutoff) t/to-default-time-zone :updated-at) ))]
        [:div.public-groups
         [:h2 "Public Groups"]
         [:h3 "Active Groups"]
         [:div.active
          (doall
            (for [group active-groups
                  :when (not (subscribed-groups (:id group)))]
              ^{:key (:id group)}
              [public-group-view group]))]
         [:h3 "Stale Groups"
          [:button.toggle-stale {:on-click (fn [_] (swap! showing? not))}
           (if @showing? "Hide" "Show")]]
         (when @showing?
           [:div.stale
            (doall
              (for [group stale-groups
                    :when (not (subscribed-groups (:id group)))]
                ^{:key (:id group)}
                [public-group-view group]))])]))))

(defn group-explore-button-view
  "Goes in the sidebar"
  []
  (let [page (subscribe [:page])
        invitations (subscribe [:invitations])]
    [:a.plus
     {:class (when (#{:group-explore :create-group} (@page :type)) "active")
      :href (routes/system-page-path {:page-id "group-explore"})}
     (when (< 0 (count @invitations))
       [:div.badge (count @invitations)])]))

(defn group-explore-page-view
  []
  [:div.page.group-explore
   [:div.title "Group Explore"]
   [:div.content
    (doall
     (for [{:keys [title url]} @group-explore-links]
       ^{:key url}
       [:a {:href url} title]))
    [invitations-view]
    [public-groups-view]]])
