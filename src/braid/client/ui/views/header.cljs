(ns braid.client.ui.views.header
  (:require [reagent.ratom :refer-macros [reaction]]
            [braid.client.routes :as routes]
            [braid.client.helpers :refer [->color]]
            [braid.client.state :refer [subscribe]]
            [braid.client.ui.views.search-bar :refer [search-bar-view]]))

(defn current-user-button-view []
  (let [user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url @user-id])
        user-nickname (subscribe [:nickname @user-id])
        open-group-id (subscribe [:open-group-id])
        current-path (subscribe [:page-path])]
    (fn []
      (let [path (routes/page-path {:group-id @open-group-id
                                    :page-id "me"})]
        [:a.user-info {:href path
                       :class (when (= current-path path) "active")}
          [:div.name (str "@" @user-nickname)]
          [:img.avatar {:src @user-avatar-url}]]))))

(defn group-name-view []
  (let [group (subscribe [:active-group])]
    (fn []
      [:div.group-name (@group :name)])))

(defn header-item-view
  [conf]
  (let [open-group-id (subscribe [:open-group-id])
        current-path (subscribe [:page-path])]
    (fn [{:keys [route-fn route-args title class body]}]
      (let [path (route-fn (merge route-args {:group-id @open-group-id}))]
        [:a {:class (str class (when (= path @current-path) " active"))
             :href path
             :title title}
         (when body body)]))))

(def left-headers
  [{:title "Inbox"
    :route-fn routes/inbox-page-path
    :class "inbox"}
   {:title "Recent"
    :route-fn routes/recent-page-path
    :class "recent"}])

(def right-headers
  [{:class "subscriptions"
    :route-fn routes/page-path
    :route-args {:page-id "tags"}
    :body "Manage Subscriptions"}
   {:class "invite-friend"
    :route-fn routes/invite-page-path
    :body "Invite a Person"}
   {:class "group-bots"
    :route-fn routes/bots-path
    :body "Bots"}
   {:class "group-uploads"
    :route-fn routes/uploads-path
    :body "Uploads"}
   {:class "edit-profile"
    :route-fn routes/page-path
    :route-args {:page-id "me"}
    :body "Edit Your Profile"}
   {:class "settings"
    :route-fn routes/group-settings-path
    :body "Settings"}])

(defn left-header-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      [:div.left {:style {:background-color (->color @group-id)}}
       [group-name-view]
       (doall
         (for [header left-headers]
           ^{:key (header :title)}
           [header-item-view header]))
       [search-bar-view]])))

(defn right-header-view []
  (let [user-id (subscribe [:user-id])]
    (fn []
      [:div.right
       [:div.bar {:style {:background-color (->color @user-id)}}
        [current-user-button-view]
        [:div.more]]
       [:div.options
        (doall
          (for [header right-headers]
            ^{:key (header :class)}
            [header-item-view header]))]])))

(defn header-view []
  [:div.header
   [left-header-view]
   [right-header-view]])
