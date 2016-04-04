(ns braid.ui.views.sidebar
  (:require [clojure.string :as string]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.routes :as routes]))

(defn groups-view []
  (let [groups (subscribe [:groups-with-unread])
        active-group (subscribe [:active-group])]
    (fn []
      [:div.groups
       (doall
         (for [group @groups]
           [:a.group.option
            {:key (group :id)
             :class (when (= (:id @active-group) (:id group)) "active")
             :style {:background-color (id->color (group :id))}
             :title (group :name)
             :href (routes/page-path {:group-id (group :id)
                                      :page-id "inbox"})}
            (string/join "" (take 2 (group :name)))
            (when-let [cnt (group :unread-count)]
              [:div.badge cnt])]))])))

(defn new-group-view []
 (let [page (subscribe [:page])]
   (fn []
     [:a.option.plus {:class (when (= (@page :type) :group-explore) "active")
                      :href (routes/other-path {:page-id "group-explore"})}])))

(defn sidebar-view []
  [:div.sidebar
   [groups-view]
   [new-group-view]])
