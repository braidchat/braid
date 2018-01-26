(ns braid.core.module-helpers
  (:require
   [clojure.tools.reader.edn :as edn]
   [clojure.java.io :as io]))

(def module-files
  (for [dir (.listFiles (io/file "src/braid/"))
        :let [m (io/file dir "module.edn")]
        :when (.isFile m)]
    m))

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
                     (apply merge)
                     vals
                     (map :fn))
        extends (->> modules
                    (mapcat (comp :cljs :extends))
                    (map second)
                    (mapcat
                      (fn [ex]
                        (cond
                          (coll? ex) ex
                          (symbol? ex) [ex]
                          :else (throw (ex-info "Invalid extends"
                                                {:thing ex}))))))]
    `(do (require
           ~@(->> (concat provides extends)
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
                    (mapcat (comp :cljs :extends))
                    (mapcat (fn [[k vs]]
                              (cond
                                (symbol? vs) [[k vs]]
                                (coll? vs) (mapv (fn [v] [k v]) vs)
                                :else (throw (ex-info "Invalid extends"
                                                      {:thing vs}))))))]
    `(do
       ~@(doall
           (for [[k v] extends]
             `(~(get-in! provides [k :fn])
               (fn [& args#]
                 (apply (resolve '~v) args#))))))))

;; TODO: make macros to define register-hooks
