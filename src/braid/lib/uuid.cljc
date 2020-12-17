(ns braid.lib.uuid
  (:require
    #?@(:clj
         [[datomic.api :as d]])))

(defn squuid []
  #?(:clj
     (d/squuid)))
