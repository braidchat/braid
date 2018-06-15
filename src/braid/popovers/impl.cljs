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
        :style {:position "absolute"
                :z-index 1000
                :top (.-top bounds)
                :left (.-left bounds)}}
       [view]])))

