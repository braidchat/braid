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
  (doseq [[k v] (helpers/get-extends :clj modules)]
    (require (symbol (namespace v)))
    ((helpers/get! provides k) (resolve! v))))

;; This is a macro so that we can make the modules be read in at
;; compile time, so that when generating an uberjar, the module
;; information is fetched at uberjar-ing time, since we won't be able
;; to access the module.edn files in the source tree at runtime
(defmacro load-modules!
  []
  `(let [modules# (quote ~(doall (helpers/read-all-modules)))]
    (apply-extends (build-provides modules#) modules#)))

(defonce inited? (atom false))

(defn init!
  []
  (when-not @inited?
    (reset! inited? true)
    (load-modules!)))
