(ns retouch.views.swipe
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [garden.core :refer [css]]
            [retouch.common :refer [easing-fn]]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [retouch.helpers :refer [debounce]]))

(def style-view
  (fn []
    [:style
     (css
       [:.container
        {:height "100%"
         :width "100%"
         :overflow "scroll"}
        [:.panels
         {:height "100%"
          :min-width "100%"
          :display #{:flex :-webkit-flex}
          :flex-flow "row"}
         [:.panel
          {:width "100%"
           :height "100%"
           :flex-shrink 0
           :display "inline-block"
           :vertical-align "top" }]]])]))

(defn swipe-view [panel-items panel-view]
  (let [state (atom {:container nil
                     :panel-count (count panel-items)
                     :dragging? false
                     :scroll-x-start nil})

        scroll-chan (chan)
        scroll-stop-chan (debounce scroll-chan 50)

        scroll-stop! (fn []
                       (let [container (@state :container)
                             width (.-offsetWidth container)
                             ;scrollWidth (.-scrollWidth container)
                             scroll-x (.-scrollLeft container)
                             start-n (/ (@state :scroll-x-start) width)
                             percent-dragged (/ (- scroll-x
                                                   (@state :scroll-x-start))
                                                width)
                             ; TODO should switch away from delta-n
                             ; instead, just based off direction and location when let go
                             delta-n (cond
                                       (< percent-dragged -1.35) -2
                                       (< percent-dragged -0.15) -1
                                       (> percent-dragged 1.35) 2
                                       (> percent-dragged 0.15) 1
                                       :else 0)
                             bound (fn [x xmin xmax]
                                     (-> x
                                         (min xmax)
                                         (max xmin)))
                             target-n (bound (+ start-n delta-n) 0 (dec (@state :panel-count)))
                             target-x (* width target-n)]

                        (set! (.-scrollLeft (@state :container)) target-x)))

        touch-end! (fn [e]
                     (swap! state assoc :dragging? false))

        touch-start! (fn [e]
                       (swap! state assoc
                              :scroll-x-start (.-scrollLeft (@state :container))
                              :dragging? true))

        scroll! (fn [e]
                  (put! scroll-chan true))

        _ (go (loop []
                (let [[_ ch] (alts! [scroll-stop-chan])]
                  (when (= ch scroll-stop-chan)
                    (when-not (@state :dragging?)
                      (scroll-stop!)))
                  (recur))))]
    (r/create-class
      {:component-did-mount
       (fn [component]
         (.addEventListener js/document "touchend" touch-end!)
         (.addEventListener js/document "touchstart" touch-start!)
         (swap! state assoc :container (r/dom-node component)))
       :component-will-update
       (fn [this new-args]
         (swap! state assoc :panel-count (count (second new-args))))
       :component-will-unmount
       (fn []
         (.removeEventListener js/document "touchend" touch-end!)
         (.removeEventListener js/document "touchstart" touch-start!))
       :reagent-render
       (fn [panel-items panel-view]
         [:div.container {:on-scroll scroll!}
          [:div.panels
           [style-view]
           (doall
             (for [panel-item panel-items]
               ^{:key (:id panel-item)}
               [:div.panel [panel-view panel-item]]))]])})))
