(ns braid.ui.views.sidebar
  (:require [clojure.string :as string]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]))

(defn badge-view [{:keys [subscribe]} group-id]
  (let [cnt (subscribe [:group-unread-count group-id])]
    (fn []
      (if (and @cnt (> @cnt 0))
        [:div.badge @cnt]
        [:div]))))

(defn groups-view [{:keys [subscribe] :as props}]
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
            (string/join "" (take 2 (group :name)))
            [badge-view props (group :id)]]))])))

(defn new-group-view [{:keys [subscribe]}]
 (let [page (subscribe [:page])]
   (fn []
     [:a.option.plus {:class (when (= (@page :type) :group-explore) "active")
                      :href (routes/other-path {:page-id "group-explore"})}])))

(defn sidebar-view [props]
  [:div.sidebar
   [groups-view props]
   [new-group-view props]])
