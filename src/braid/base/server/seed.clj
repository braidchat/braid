(ns braid.base.server.seed
  (:require
    [braid.core.hooks :as hooks]))

(defonce seed-fns (hooks/register! (atom [])))

(defn seed! []
  (when (empty? @seed-fns)
    "Nothing seeded. You may need to initialize modules first.")
  (doseq [f @seed-fns]
    (f)))


