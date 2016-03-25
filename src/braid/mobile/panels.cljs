(ns braid.mobile.panels
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [garden.core :refer [css]]))

(def easing-fn
  "cubic-bezier(.42, 0, .58, 1)")

(def style-view
  (fn []
    [:style
     (css
       [:.panels
        {:height "100vh"
         :display "flex"
         :flex-flow "row"}
        [:.panel
         {:width "100vw"
          :height "100vh"
          :display "inline-block"
          :vertical-align "top" }]])]))

(defn panels-view [panel-items panel-view]
  (let [state (r/atom {:pos-x 0})

        px->vw (fn [px]
                 (* 100 (/ px (.-innerWidth js/window))))

        drag-start! (fn [touch-x]
                      (swap! state assoc
                             :pos-x-start (@state :pos-x)
                             :touch-x-start touch-x
                             :touch-x touch-x
                             :dragging? true))

        drag-end! (fn []
                    (let [w (.-innerWidth js/window)
                          pos-n (- (/ (@state :pos-x-start) w))
                          delta-n (let [percent-dragged (/ (- (@state :pos-x)
                                                              (@state :pos-x-start))
                                                           w)]
                                    (cond
                                      (< percent-dragged -0.15) 1
                                      (> percent-dragged 0.15) -1
                                      :else 0))
                          bound (fn [x xmin xmax]
                                  (-> x
                                      (min xmax)
                                      (max xmin)))
                          max-n (dec (@state :panel-count))
                          new-n (bound (+ pos-n delta-n) 0 max-n)
                          snap-x (- (* w new-n))]

                      (swap! state assoc
                             :dragging? false
                             :pos-x snap-x)))

        set-touch-x! (fn [touch-x]
                       (swap! state assoc
                              :touch-x touch-x
                              :pos-x (+ (@state :pos-x-start)
                                        (- (@state :touch-x)
                                           (@state :touch-x-start)))))

        set-panel-count! (fn [panel-count]
                           (swap! state assoc :panel-count panel-count))

        get-dragging? (fn []
                        (reaction (@state :dragging?)))

        get-pos-x (fn [] (reaction (@state :pos-x)))

        touch-start! (fn [e]
                       (let [x (.-clientX (aget (.-touches e) 0))]
                         (drag-start! x)))

        touch-move! (fn [e]
                      (let [x (.-clientX (aget (.-touches e) 0))
                            dragging? (get-dragging?)]
                        (when @dragging?
                          (set-touch-x! x))))

        touch-end! (fn [e]
                     (let [x (.-clientX (aget (.-changedTouches e) 0))
                           dragging? (get-dragging?)]
                       (when @dragging?
                         (drag-end!))))]
    (r/create-class
      {:component-did-mount
       (fn []
         (.addEventListener js/document "touchstart" touch-start!)
         (.addEventListener js/document "touchmove" touch-move!)
         (.addEventListener js/document "touchend" touch-end!))
       :component-will-update
       (fn [this _]
         (set-panel-count! (count (first (r/children this)))))
       :component-will-unmount
       (fn []
         (.removeEventListener js/document "touchstart" touch-start!)
         (.removeEventListener js/document "touchmove" touch-move!)
         (.removeEventListener js/document "touchend" touch-end!))
       :reagent-render
       (fn [panel-items panel-view]
         (let [x (get-pos-x)
               dragging? (get-dragging?)]
           [:div.panels {:style {:transform (str "translate3d(" @x "px,0,0)")
                                 :transition (when-not @dragging?
                                               (str "transform" " 0.25s " easing-fn))}}
            [style-view]
            (doall
              (for [panel-item panel-items]
                ^{:key (:id panel-item)}
                [:div.panel [panel-view panel-item]]))]))})))
