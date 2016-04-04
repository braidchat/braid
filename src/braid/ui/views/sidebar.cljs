(ns braid.ui.views.sidebar
  (:require [clojure.string :as string]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]))

(defn groups-view [{:keys [subscribe]}]
  (let [groups (subscribe [:groups-with-unread])
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
            (string/join "" (take 2 (group :name)))
            (when-let [cnt (group :unread-count)]
              [:div.badge cnt])]))])))

(defn new-group-view [{:keys [subscribe]}]
 (let [page (subscribe [:page])]
   (fn []
     [:a.option.plus {:class (when (= (@page :type) :group-explore) "active")
                      :href (routes/other-path {:page-id "group-explore"})}])))

(defn sidebar-view [props]
  [:div.sidebar
   [groups-view props]
   [new-group-view props]])
