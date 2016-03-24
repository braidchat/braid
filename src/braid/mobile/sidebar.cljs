(ns braid.mobile.sidebar
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]))

(defn groups-view []
  (let [groups (subscribe [:groups])]
    [:div.groups
     (for [group @groups]
       ^{:key (group :id)}
       [:a.group {:on-click
                  (fn [e]
                    (dispatch [:set-active-group-id! (group :id)])
                    (dispatch [:sidebar-close!]))}
        [:img]
        [:div.name (group :name)]])]))

(defn sidebar-view []
  (let [sidebar-position (subscribe [:sidebar-position])
        sidebar-open? (subscribe [:sidebar-open?])
        sidebar-dragging? (subscribe [:sidebar-dragging?])]
    (r/create-class
      {:component-did-mount
       (fn []
         (let [open-width 100 ; TODO grab this dynamically from .content width
               fudge-x 50]
           (.addEventListener js/document "touchstart"
                              (fn [e]
                                (let [x (.-clientX (aget (.-touches e) 0))
                                      sidebar-open? (subscribe [:sidebar-open?])]
                                  (when (or (and (not @sidebar-open?) (< x fudge-x))
                                            (and @sidebar-open? (< (- open-width fudge-x) x (+ open-width fudge-x))))
                                    (dispatch [:sidebar-drag-start! x])))))

           (.addEventListener js/document "touchmove"
                              (fn [e]
                                (let [x (.-clientX (aget (.-touches e) 0))
                                      sidebar-open? (subscribe [:sidebar-open?])]
                                  (when @sidebar-dragging?
                                    (dispatch [:sidebar-set-position! x]))))
                              false)

           (.addEventListener js/document "touchend"
                              (fn [e]
                                (let [x (.-clientX (aget (.-changedTouches e) 0))
                                      sidebar-dragging? (subscribe [:sidebar-dragging?])]
                                  (when @sidebar-dragging?
                                    (if @sidebar-open?
                                      (if (< x open-width)
                                        (dispatch [:sidebar-close!])
                                        (dispatch [:sidebar-open!]))
                                      (if (> x fudge-x)
                                        (dispatch [:sidebar-open!])
                                        (dispatch [:sidebar-close!]))))))
                              false)))
       :component-will-unmount
       (fn []
         ; TODO remove listeners
         )
       :reagent-render
       (fn []
         [:div.sidebar
          (cond
            @sidebar-dragging?
            {:style {:transform (str "translateX(" @sidebar-position "px)")}}
            @sidebar-open?
            {:class "open"}
            :else
            {:class "closed"})
          [:div.content
           [groups-view]]])})))
