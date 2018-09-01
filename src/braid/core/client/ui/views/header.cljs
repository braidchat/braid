(ns braid.core.client.ui.views.header
  (:require
    [re-frame.core :refer [subscribe dispatch]]
    [braid.core.hooks :as hooks]
    [braid.core.client.helpers :refer [->color]]
    [braid.core.client.routes :as routes]
    [braid.core.client.ui.views.search-bar :refer [search-bar-view]]
    [braid.core.client.ui.views.user-header :refer [user-header-view]]
    [braid.core.client.ui.views.header-item :refer [header-item-view HeaderItem]]))

(defn loading-indicator-view [group-id]
  (let [page (subscribe [:page])]
    (fn [group-id]
      [:div.loading-indicator
       {:class (cond
                 (@page :loading?) "loading"
                 (@page :error?) "error")
        :style {:color (->color group-id)}}])))

(defn group-name-view []
  (let [group (subscribe [:active-group])]
    (fn []
      [:div.group-name (@group :name)])))

(def GroupHeaderItem HeaderItem)

(defonce group-header-buttons
  (hooks/register!
    (atom
      [{:title "Inbox"
        :route-fn routes/group-page-path
        :route-args {:page-id "inbox"}
        :icon \uf01c
        :priority 10}
       {:title "Recently Closed"
        :route-fn routes/recent-page-path
        :icon \uf1da
        :priority 5}])
    [GroupHeaderItem]))

(defn group-header-buttons-view [header-items]
  [:div.buttons
   (doall
     (for [header-item header-items]
       ^{:key (header-item :title)}
       [header-item-view header-item]))])

(defn group-header-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      [:div.group-header
       [:div.bar {:style {:background-color (->color @group-id)}}
        [group-name-view]
        [group-header-buttons-view (->> @group-header-buttons
                                        (sort-by :priority)
                                        reverse)]
        [search-bar-view]]
       [loading-indicator-view @group-id]])))

(def admin-header-items
  [{:class "settings"
    :route-fn routes/group-settings-path
    :body "Group Settings"}
   {:class "group-bots"
    :route-fn routes/bots-path
    :body "Bots"}])

(defn admin-header-view []
  (let [user-id (subscribe [:user-id])
        open-group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [open-group-id])]
    (fn []
      (when @admin?
        [:div.admin-header
         [:div.admin-icon {:style {:color (->color @user-id)}}]
         [:div.options
          [:div.content
           (doall
             (for [header-item admin-header-items]
               ^{:key (header-item :class)}
               [header-item-view header-item]))]]]))))

(defonce header-views
  (hooks/register! (atom []) [fn?]))

(defn readonly-header-view
  []
  (let [group @(subscribe [:active-group])]
    [:div.header
     [:div.group-header
      [:div.bar {:style {:background-color (->color (group :id))}}
       [:div.group-name (group :name)]]]
     [:div.user-header
      [:div.bar {:style {:background-color
                         (->color (or @(subscribe [:user-id]) (group :id)))}}
       [:button.join
        {:on-click (fn [_] (dispatch [:core/join-public-group (:id group)]))}
        "Join Group"]]]]))

(defn logged-in-header-view
  []
  (into
    [:div.header]
    (concat
      [[group-header-view]
       [:div.spacer]]
      (doall
        (for [view @header-views]
          [view]))
      [[admin-header-view]
       [user-header-view]])))

(defn header-view
  []
  (if (:readonly @(subscribe [:active-group]))
    [readonly-header-view]
    [logged-in-header-view]))
