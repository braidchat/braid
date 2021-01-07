(ns braid.lib.uuid
  (:require
    #?@(:clj
         [[datomic.api :as d]]
         :cljs
         [[cljs-uuid-utils.core :as uuid]])))

(defn squuid []
  #?(:clj
     (d/squuid)
     :cljs
     (uuid/make-random-squuid)))
