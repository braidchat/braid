(ns braid.popovers.api
  (:require
    [braid.base.api :as base]))

(defn register-popover-styles! [styles]
  (base/register-styles!
    [:#app>.app>.main
     [:>.popover
      styles]]))
