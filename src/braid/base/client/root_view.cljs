(ns braid.base.client.root-view
  (:require
    [braid.core.hooks :as hooks]))

(defonce root-views (hooks/register! (atom []) [fn?]))

