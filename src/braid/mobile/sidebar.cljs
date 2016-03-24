(ns braid.mobile.sidebar
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]))

(defn sidebar-view [content-view]
  (let [state (r/atom {})

        get-position (fn []
                       (reaction (@state :position)))
        get-open? (fn []
                    (reaction (@state :open?)))
        get-dragging?  (fn []
                         (reaction (@state :dragging?)))

        open! (fn []
                (swap! state assoc
                       :open? true
                       :dragging? false))

        close!  (fn []
                  (swap! state assoc
                         :open? false
                         :dragging? false))

        drag-start! (fn [x]
                      (swap! state assoc
                             :dragging? true
                             :position x))

        set-position! (fn [x]
                        (swap! state assoc :position x))

        open-width 100 ; TODO grab this dynamically from .content width
        fudge-x 50

        touch-start!  (fn [e]
                        (let [x (.-clientX (aget (.-touches e) 0))
                              open? (get-open?)]
                          (when (or (and (not @open?) (< x fudge-x))
                                    (and @open? (< (- open-width fudge-x) x (+ open-width fudge-x))))
                            (.stopPropagation e)
                            (drag-start! x))))

        touch-move! (fn [e]
                      (let [x (.-clientX (aget (.-touches e) 0))
                            dragging? (get-dragging?)]
                        (when @dragging?
                          (.stopPropagation e))
                        (set-position! x)))

        touch-end!  (fn [e]
                      (let [x (.-clientX (aget (.-changedTouches e) 0))
                            dragging? (get-dragging?)
                            open? (get-open?)]
                        (when @dragging?
                          (.stopPropagation e)
                          (if @open?
                            (if (< x open-width)
                              (close!)
                              (open!))
                            (if (> x fudge-x)
                              (open!)
                              (close!))))))]
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
       (fn [content-view]
         (let [dragging? (get-dragging?)
               open? (get-open?)
               position (get-position)]
           [:div.sidebar (cond
                           @dragging?
                           {:style {:transform (str "translateX(" @position "px)")}}
                           @open?
                           {:class "open"}
                           :else
                           {:class "closed"})
            [:div.content content-view]]))})))
