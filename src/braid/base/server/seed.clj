(ns braid.base.server.seed
  (:require
    [braid.core.hooks :as hooks]
    [braid.core.server.db :as db]))

(defonce seed-txns (hooks/register! (atom [])))

(defn seed! []
  (when (empty? @seed-txns)
    "Nothing seeded. You may need to initialize modules first.")
  (doseq [f @seed-txns]
    (doseq [[label txns] (f)]
      (println label)
      (db/run-txns! txns))))


