(ns braid.popovers.helpers
  (:require
    [braid.popovers.impl :as impl]))

(defn on-mouse-enter [view]
  (fn [e]
    (reset! impl/popover
            {:target (.-currentTarget e)
             :view view})))
