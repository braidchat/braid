(ns braid.base.server.initial-data
  (:require
    [braid.core.hooks :as hooks]))

(defonce initial-user-data (hooks/register! (atom [])))
