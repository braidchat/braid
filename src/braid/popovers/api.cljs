(ns braid.popovers.api
  (:require
    [braid.core.api :as core]))

(defn register-popover-styles! [styles]
  (core/register-styles!
    [:#app>.app>.main
     [:>.popover
      styles]]))
