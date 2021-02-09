(ns braid.base.server.cqrs-fx
  "Namespace that collects common CQRS command fx"
  (:require
    [braid.base.server.socket :as socket]
    [braid.core.server.sync-helpers :as helpers]
    [braid.core.server.db :as db]))

(def run-txns! db/run-txns!)

(def chsk-send! socket/chsk-send!)

(def group-broadcast! helpers/broadcast-group-change)
