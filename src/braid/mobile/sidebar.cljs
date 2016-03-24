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
        sidebar-dragging? (subscribe [:sidebar-dragging?])

        open-width 100 ; TODO grab this dynamically from .content width
        fudge-x 50

        touch-start!  (fn [e]
                        (let [x (.-clientX (aget (.-touches e) 0))
                              sidebar-open? (subscribe [:sidebar-open?])]
                          (when (or (and (not @sidebar-open?) (< x fudge-x))
                                    (and @sidebar-open? (< (- open-width fudge-x) x (+ open-width fudge-x))))
                            (.stopPropagation e)
                            (dispatch [:sidebar-drag-start! x]))))

        touch-move! (fn [e]
                      (let [x (.-clientX (aget (.-touches e) 0))
                            sidebar-dragging? (subscribe [:sidebar-dragging?])]
                        (when @sidebar-dragging?
                          (.stopPropagation e))
                        (dispatch [:sidebar-set-position! x])))

        touch-end!  (fn [e]
                      (let [x (.-clientX (aget (.-changedTouches e) 0))
                            sidebar-dragging? (subscribe [:sidebar-dragging?])]
                        (when @sidebar-dragging?
                          (.stopPropagation e)
                          (if @sidebar-open?
                            (if (< x open-width)
                              (dispatch [:sidebar-close!])
                              (dispatch [:sidebar-open!]))
                            (if (> x fudge-x)
                              (dispatch [:sidebar-open!])
                              (dispatch [:sidebar-close!]))))))
        ]
    (r/create-class
      {:component-did-mount
       (fn []
         (.addEventListener js/document "touchstart" touch-start! true)
         (.addEventListener js/document "touchmove" touch-move!  true)
         (.addEventListener js/document "touchend" touch-end! true))
       :component-will-unmount
       (fn []
         (.removeEventListener js/document "touchstart" touch-start! true)
         (.removeEventListener js/document "touchmove" touch-move!  true)
         (.removeEventListener js/document "touchend" touch-end! true))
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
