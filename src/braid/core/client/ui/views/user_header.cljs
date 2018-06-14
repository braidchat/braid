(ns braid.core.client.ui.views.user-header
  (:require
    [re-frame.core :refer [subscribe dispatch]]
    [braid.core.hooks :as hooks]
    [braid.core.client.helpers :as helpers]
    [braid.core.client.routes :as routes]
    [braid.core.client.ui.views.header-item :refer [header-item-view HeaderItem]]))

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

(def UserHeaderItem HeaderItem)

(defonce user-header-menu-items
  (hooks/register!
    (atom
      [{:body "Manage Subscriptions"
        :route-fn routes/page-path
        :route-args {:page-id "tags"}
        :icon \uf02c
        :priority 10}
       {:body "Invite a Person"
        :route-fn routes/invite-page-path
        :icon \uf1e0
        :priority 9}
       {:route-fn routes/page-path
        :route-args {:page-id "me"}
        :body "Edit Your Profile"
        :icon \uf2bd
        :priority 8}
       #_{:body "See Changelog"
          :route-fn routes/page-path
          :route-args {:page-id "changelog"}
          :icon \uf1da
          :priority 6}])
    [UserHeaderItem]))

(defn user-header-view []
  (let [user-id (subscribe [:user-id])]
    (fn []
      [:div.user-header
       [:div.bar {:style {:background-color (helpers/->color @user-id)}}
        [current-user-button-view]
        [:div.more]]
       [:div.options
        (into
          [:div.content]
          (doall
            (for [header-item (->> @user-header-menu-items
                                   (sort-by :priority)
                                   reverse)]
              ^{:key (header-item :class)}
              [header-item-view header-item])))]])))
