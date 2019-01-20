(ns braid.popovers.impl
  (:require
    [reagent.core :as r]))

(def popover (r/atom nil))

(defn view []
  (when-let [{:keys [view target]} @popover]
    (let [bounds (.getBoundingClientRect target)]
      [:div.popover
       {:on-mouse-leave (fn []
                          (reset! popover nil))
        :on-touch-start (fn [e]
                          (.stopPropagation e)
                          (when (= (.-target e) (.-currentTarget e))
                            (reset! popover nil)))
        :style {:position "absolute"
                :z-index 1000
                :width (.-width bounds)
                :height (.-height bounds)
                :top (+ (.-scrollY js/window) (.-top bounds))
                :left (+ (.-scrollX js/window) (.-left bounds))}}
       [view]])))
