(ns braid.core.modules
  (:require
   [braid.core.module-helpers :as helpers]))

(defn resolve!
  [s]
  (or (resolve s) (throw (ex-info (str "Couldn't resolve " s)
                                  {:symbol s}))))

(defn build-provides
  [modules]
  (->> (map (comp :clj :provides) modules)
      (apply merge)
      (reduce-kv
        (fn [m k {f-name :fn}]
          (require (symbol (namespace f-name)))
          (assoc m k (resolve! f-name)))
        {})))

(defn apply-extends
  [provides modules]
  (doseq [[k v] (mapcat (comp :clj :extends) modules)]
    (require (symbol (namespace v)))
    ((helpers/get! provides k) (deref (resolve! v)))))

(defn load-modules!
  []
  (let [modules (helpers/read-all-modules)]
    (apply-extends (build-provides modules) modules)))

(load-modules!)
