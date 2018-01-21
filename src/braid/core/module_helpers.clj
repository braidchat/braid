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
  "Generate the `require` statements to pull in all necessary
  namespaces to be able to connect the modules. That is, all the
  namespaces named in both `:extends` and `:provides`."
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
  "Generate the function calls needed to connect up the `:extend`-ing
  and `:provide`-ing modules.
  Doing this with a macro instead of a function because we want the
  parsing of the EDN files to happen at compile time, by Clojure,
  rather than at runtime with ClojureScript (particularly because
  ClojureScript can't actually read said module.edn files)."
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
