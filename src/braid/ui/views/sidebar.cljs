(ns braid.ui.views.sidebar
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [chat.client.views.helpers :refer [id->color location]]
            [chat.client.routes :as routes]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.styles.sidebar :as side-style]
            [goog.events :as events]
            [goog.style :as gstyle])
  (:import [goog.events EventType]))

(defn badge-view [group-id]
  (let [cnt (subscribe [:group-unread-count group-id])]
    (fn []
      (if (and @cnt (> @cnt 0))
        [:div.badge @cnt]
        [:div]))))

(defn groups-view []
  (let [groups (subscribe [:groups])
        active-group (subscribe [:active-group])
        group-order (subscribe [:user-preference :groups-order])
        dragging-elt (r/atom nil)
        listeners (r/atom nil)
        drag-start (r/atom nil)
        drag-move (fn [elt e])
        drag-end (fn [elt e]
                   (reset! dragging-elt nil)
                   (let [[up move] @listeners]
                     (doto js/window
                       (events/listen EventType.MOUSEUP up)
                       (events/listen EventType.MOUSEMOVE move))))
        drag-start (fn [elt e]
                     (reset! dragging-elt elt)
                     (reset! drag-start (location (.-target e)))
                     (let [up (partial drag-end elt)
                           move (partial drag-move elt)]
                       (reset! listeners [up move])
                       (doto js/window
                         (events/listen EventType.MOUSEUP up)
                         (events/listen EventType.MOUSEMOVE move))))]
    (fn []
      [:div.groups
       (let [ordered-groups (if (nil? @group-order)
                              @groups
                              (let [ordered? (comp (set @group-order) :id)
                                    {ord true unord false} (group-by ordered? @groups)
                                    by-id (group-by :id @groups)]
                                (concat
                                  (map by-id @group-order)
                                  unord)))]
         (doall
           (for [group ordered-groups]
             ^{:key (group :id)}
             [:a.group.option
              {:class (when (= (:id @active-group) (:id group)) "active")
               :style (merge
                        {:background-color (id->color (group :id))}
                        (when-let [avatar (group :avatar)]
                          {:background-image (str "url(" avatar ")")
                           :background-position "center"
                           :background-size "100%"}))
               :title (group :name)
               :href (routes/page-path {:group-id (group :id)
                                        :page-id "inbox"})
               :on-mouse-down (partial drag-start group)
               :on-mouse-up (partial drag-end group)
               :on-mouse-move (partial drag-move group)}
              (when-not (group :avatar)
                (string/join "" (take 2 (group :name))))
              [badge-view (group :id)]])))])))

(defn new-group-view []
 (let [page (subscribe [:page])]
   (fn []
     [:a.option.plus {:class (when (= (@page :type) :group-explore) "active")
                      :href (routes/other-path {:page-id "group-explore"})}])))

(defn sidebar-view []
  [:div.sidebar
   [groups-view]
   [new-group-view]])
