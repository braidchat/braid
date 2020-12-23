(ns braid.sidebar.core
  (:require
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.sidebar.styles :as styles]
          [braid.sidebar.ui :as ui]])))

(defn init! []
  #?(:cljs
     (do
       (base/register-styles!
         [:.main
          styles/>sidebar])

       (base/register-root-view!
         ui/sidebar-view))))
