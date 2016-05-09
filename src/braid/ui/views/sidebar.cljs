(ns braid.ui.views.sidebar
  (:require [clojure.string :as string]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.reagent-adapter :refer [subscribe]]))

(defn badge-view [group-id]
  (let [cnt (subscribe [:group-unread-count group-id])]
    (fn []
      (if (and @cnt (> @cnt 0))
        [:div.badge @cnt]
        [:div]))))

(defn groups-view []
  (let [groups (subscribe [:groups])
        active-group (subscribe [:active-group])]
    (fn []
      [:div.groups
       (doall
         (for [group @groups]
           ^{:key (group :id)}
           [:a.group.option
            {:class (when (= (:id @active-group) (:id group)) "active")
             :style {:background-color (id->color (group :id))}
             :title (group :name)
             :href (routes/page-path {:group-id (group :id)
                                      :page-id "inbox"})}
            [:span.title (string/join "" (take 2 (group :name)))]
            (when-let [avatar (group :avatar)]
              [:img.avatar {:src avatar}])
            [badge-view (group :id)]]))])))

(defn new-group-view []
 (let [page (subscribe [:page])]
   (fn []
     [:a.option.plus {:class (when (= (@page :type) :group-explore) "active")
                      :href (routes/other-path {:page-id "group-explore"})}])))

(defn sidebar-view []
  [:div.sidebar
   [groups-view]
   [new-group-view]])
