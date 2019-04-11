(ns braid.popovers.helpers
  (:require
    [braid.popovers.impl :as impl]))

(defn on-mouse-enter [view]
  (fn [e]
    (reset! impl/popover
            {:target (.-currentTarget e)
             :view view})))

(defn on-touch-start
  [view]
  (fn [e]
    (reset! impl/popover {:target (.-currentTarget e)
                          :view view})))

(defn on-click
  [view]
  (fn [e]
    (reset! impl/popover {:target (.-currentTarget e)
                          :view view
                          :modal? true})))

(defn close!
  []
  (reset! impl/popover nil))
