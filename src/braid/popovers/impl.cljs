(ns braid.popovers.impl
  (:require
    [reagent.core :as r]))

(def popover (r/atom nil))

(defn view []
  (when-let [{:keys [view target modal?]} @popover]
    (let [bounds (if modal?
                   #js {:width js/document.documentElement.clientWidth
                        :height js/document.documentElement.clientHeight
                        :left js/document.documentElement.clientLeft
                        :top js/document.documentElement.clientTop}
                   (.getBoundingClientRect target))]
      [:div.popover
       {:on-mouse-leave (fn []
                          (when-not modal?
                            (reset! popover nil)))
        :on-click (fn [e]
                    (.stopPropagation e)
                    (when (= (.-target e) (.-currentTarget e))
                      (reset! popover nil)))
        :on-touch-start (fn [e]
                          (.stopPropagation e)
                          (when (= (.-target e) (.-currentTarget e))
                            (reset! popover nil)))
        :style
        (merge {:position "absolute"
                :z-index 10000
                :width (.-width bounds)
                :height (.-height bounds)
                :top (+ (.-scrollY js/window) (.-top bounds))
                :left (+ (.-scrollX js/window) (.-left bounds))}
               (when modal?
                 {:background-color "rgba(0, 0, 0, 0.5)"
                  :display "flex"
                  :justify-content "center"
                  :align-items "center"}))}

       [view]])))
