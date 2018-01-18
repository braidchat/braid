(ns braid.core.module-helpers
  (:require
   [clojure.tools.reader.edn :as edn]))

(def module-files
  ;; TODO: glob, have a search path, have a registery or something
  ["src/braid/core/module.edn"
   "src/braid/quests/module.edn"
   "src/braid/emoji/module.edn"])

(defn read-module
  [module-path]
  (edn/read-string (slurp module-path)))

(defn read-all-modules
  []
  (map read-module module-files))

(defn get! [m k]
  (let [v (get m k ::explode)]
    (if (not= v ::explode)
      v
      (throw (ex-info (str "Missing key " k)
                      {:map m :key k})))))

(defn get-in! [m ks]
  (let [v (get-in m ks ::explode)]
    (if (not= v ::explode)
      v
      (throw (ex-info (str "Missing key-path " ks)
                      {:map m :keys ks})))))

(defmacro gen-module-requires
  []
  (let [modules (read-all-modules)
        provides (->> modules
                     (map (comp :cljs :provides))
                     (apply merge))
        extends (->> modules
                    (mapcat (comp :cljs :extends)))]
    `(do (require
           ~@(->> (concat (map :fn (vals provides))
                         (map second extends))
                 (map (comp symbol namespace))
                 set
                 (map (partial list 'quote)))))))

(defmacro gen-module-loads
  []
  (let [modules (read-all-modules)
        provides (->> modules
                     (map (comp :cljs :provides))
                     (apply merge))
        extends (->> modules
                    (mapcat (comp :cljs :extends)))]
    `(do
       ~@(doall
           (for [[k v] extends]
             `(~(get-in! provides [k :fn]) ~v))))))
