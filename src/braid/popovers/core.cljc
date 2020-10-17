(ns braid.popovers.core
  "Allows modules to render popovers.

  Register styles with:
    braid.popovers.register-popover-styles!

  Trigger popovers by:
   {:on-mouse-enter
    (braid.popovers.helpers/on-mouse-enter
      (fn []
        [view-to-show]))}"
  (:require
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.popovers.impl :as impl]])))

(defn init! []
  #?(:cljs
     (do
       (base/register-root-view! impl/view))))
