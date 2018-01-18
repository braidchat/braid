(ns braid.core.modules
  "This namespace exist just to require other modules' defstate.
   It is required by braid.server.core"
  (:require
   [braid.quests.server.core]
   [clojure.edn :as edn]))

(def module-files
  ;; TODO: glob, have a search path, have a registery or something
  ["src/braid/core/module.edn" "src/braid/quests/module.edn"])

(defn read-module
  [module-path]
  (edn/read-string (slurp module-path)))

(defn read-all-modules
  []
  (map read-module module-files))

(defn resolve!
  [s]
  (or (resolve s) (throw (ex-info (str "Couldn't resolve " s)
                                  {:symbol s}))))

(defn build-provides
  [modules]
  (->> (map :provides modules)
      (apply merge)
      (reduce-kv
        (fn [m k {f-name :fn}]
          (require (symbol (namespace f-name)))
          (assoc m k (resolve! f-name)))
        {})))

(defn get! [m k]
  (let [v (get m k ::explode)]
    (if (not= v ::explode)
      v
      (throw (ex-info (str "Missing key " k)
                      {:map m :key k})))))

(defn apply-extends
  [provides modules]
  (doseq [[k v] (mapcat :extends modules)]
    (require (symbol (namespace v)))
    ((get! provides k) (deref (resolve! v)))))

(defn load-modules!
  []
  (let [modules (read-all-modules)]
    (apply-extends (build-provides modules) modules)))

(load-modules!)
