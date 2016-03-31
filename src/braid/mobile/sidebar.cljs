(ns braid.mobile.sidebar
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [garden.core :refer [css]]))

(def easing-fn
  "cubic-bezier(.42, 0, .58, 1)")

(def style-view
  (fn []
    [:style
     (css
       (let [w "25vw"]
         [:.sidebar
          {:background "black"
           :position "absolute"
           :top 0
           :bottom 0
           :padding-left "100vw"
           :margin-left "-100vw"
           :width w
           :right "100vw"
           :z-index 100}
          [:&.open :&.closed
           {:transition (str "transform" " 0.25s " easing-fn)}]
          [:&.closed
           {:transform "translate3d(0,0,0)"}]
          [:&.open
           {:transform (str "translate3d(" w ",0,0)")}]]))]))

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
                          (when (and (not @open?) (< x fudge-x))
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
                        (if (not @dragging?)
                          (close!)
                          (do
                            (.stopPropagation e)
                            (if @open?
                              (if (< x open-width)
                                (close!)
                                (open!))
                              (if (> x fudge-x)
                                (open!)
                                (close!)))))))]
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
                           {:style {:transform (str "translate3d(" @position "px, 0, 0)")}}
                           @open?
                           {:class "open"}
                           :else
                           {:class "closed"})
            [:div.content content-view]
            [style-view]]))})))
