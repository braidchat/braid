(ns braid.core.module-helpers
  "Helpers for the Braid module system.
  The important things here are:
  - The `defhook` macro, which Clojure & Clojurescript should use to
    define extension points for modules
  - The `gen-module-requires` and `gen-module-loads` macros, which
    `modules.cljs` uses to set up cljs module extensions"
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

(defn get-extends
  "Get all the `:extends` key-value pairs, expanding any collections.
  e.g. `{:register-foo [foo bar]}` will expand to
  `([:register-foo foo] [:register-foo bar])`."
  [target modules]
  (into []
        (comp (mapcat (comp target :extends))
              (mapcat (fn [[k vs]]
                        (cond
                          (symbol? vs) [[k vs]]
                          (coll? vs) (mapv (fn [v] [k v]) vs)
                          :else (throw (ex-info (str "Invalid extends " vs)
                                                {:form vs
                                                 :target target
                                                 :modules modules}))))))
        modules))

(defmacro gen-module-requires
  "Generate the `require` statements to pull in all necessary
  namespaces to be able to connect the modules. That is, all the
  namespaces named in both `:extends` and `:provides`.
  It seems that this should be in a separate namespace from
  `gen-module-loads`, or the namespaces won't yet be loaded when the
  extension calls are made."
  []
  (let [modules (read-all-modules)
        provides (->> modules
                     (map (comp :cljs :provides))
                     (apply merge)
                     vals
                     (map :fn))
        extends (->> modules
                    (get-extends :cljs)
                    (map second))]
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
  ClojureScript can't actually read said module.edn files).
  This should be in a namespace that requires another ns that invokes
  the `gen-module-requires` macro."
  []
  (let [modules (read-all-modules)
        provides (->> modules
                     (map (comp :cljs :provides))
                     (apply merge))
        extends (get-extends :cljs modules)]
    `(do
       ~@(doall
           (for [[k v] extends]
             `(~(get-in! provides [k :fn])
               (fn []
                 (deref (resolve '~v)))))))))

(defmacro defhook
  "Macro to generate a module extension point.
  `:writer` will be a function name that should be in the `:provides`
  section of the module.edn.
  `:reader` will be deref-able and will give a collection of the
  values passed to the writer.

  By default, the values will be `conj`-ed into a vector, but this can
  be customized with the `:initial-value` and `:add-fn` arguments."
  [& {:keys [reader writer initial-value add-fn]
      :or {initial-value [] add-fn 'conj}}]
  (assert (symbol? reader))
  (assert (symbol? writer))
  ;; This is a macro, so it'll always be Clojure, so we can't just use
  ;; a reader conditional, so we check `(:ns &env)`, which will be
  ;; `nil` if evaluating the macro in Clojure.
  (let [clojure? (nil? (:ns &env))]
    `(do
       (defonce extend-thunks# (atom []))
       (def ~reader
         ;; Hack around protocol being clojure.lang.IDeref in Clojure vs
         ;; cljs.core/IDeref in Clojurescript and the method being
         ;; `deref` in Clojure vs `-deref` in Clojurescript.
         (reify ~(if clojure? 'clojure.lang.IDeref
                   'cljs.core/IDeref)
           (~(if clojure? 'deref '-deref)
            [_#]
            (reduce
              (fn [vs# th#]
                ;; In Clojure the module-loading will store the Vars
                ;; themselves so we can just deref them, but in
                ;; Clojurescript vars aren't reified at runtime, so
                ;; module loading passes thunks
                (~add-fn vs# (~(if clojure?
                                 `deref
                                 `(fn [x#] (x#)))
                              th#)))
              ~initial-value
              (deref extend-thunks#)))))
       (defn ~writer [f#]
         (when-not (~(if clojure? `var? `fn?) f#)
           (throw (ex-info
                    (str "Extensions must be "
                         ~(if clojure? "var" "functions"))
                    {:invalid-value f#
                     :register-fn (quote ~writer)})))
         (swap! extend-thunks# conj f#)))))
