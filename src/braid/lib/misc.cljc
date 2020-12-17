(ns braid.lib.misc)

(defn key-by [k coll]
  (into {} (map (juxt k identity)) coll))
