(ns braid.core.client.ui.views.user-header
  (:require
    [re-frame.core :refer [subscribe]]
    [braid.core.client.helpers :as helpers]
    [braid.core.client.routes :as routes]
    [braid.core.client.ui.views.header-item :refer [header-item-view]]))

(defn current-user-button-view []
  (let [user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url] [user-id])
        user-nickname (subscribe [:nickname] [user-id])
        open-group-id (subscribe [:open-group-id])
        current-path (subscribe [:page-path])]
    (fn []
      (let [path (routes/page-path {:group-id @open-group-id
                                    :page-id "me"})]
        [:a.user-info {:href path
                       :class (when (= current-path path) "active")}
         [:div.name (str "@" @user-nickname)]
         [:img.avatar {:src @user-avatar-url}]]))))

(def user-header-items
  [{:class "subscriptions"
    :route-fn routes/page-path
    :route-args {:page-id "tags"}
    :body "Manage Subscriptions"}
   {:class "invite-friend"
    :route-fn routes/invite-page-path
    :body "Invite a Person"}
   {:class "edit-profile"
    :route-fn routes/page-path
    :route-args {:page-id "me"}
    :body "Edit Your Profile"}
   #_{:class "changelog"
    :route-fn routes/page-path
    :route-args {:page-id "changelog"}
    :body "See Changelog"}])

(defn user-header-view []
  (let [user-id (subscribe [:user-id])]
    (fn []
      [:div.user-header
       [:div.bar {:style {:background-color (helpers/->color @user-id)}}
        [current-user-button-view]
        [:div.more]]
       [:div.options
        [:div.content
         (doall
           (for [header-item user-header-items]
             ^{:key (header-item :class)}
             [header-item-view header-item]))]]])))
