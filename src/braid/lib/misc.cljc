(ns braid.lib.misc)

(defn key-by [k coll]
  (into {} (map (juxt k identity)) coll))

(defn flip
  "Partially apply the function f to the given args, which will come after the
  next args.  i.e. ((flip vector 3 4) 1 2) => [1 2 3 4]"
  [f & a]
  (fn [& b]
    (apply f (concat b a))))
