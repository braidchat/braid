(ns braid.server.cache
  (:require [environ.core :refer [env]]
            [taoensso.carmine :as car]))

; same as conf in handler, but w/e
(def redis-conn {:pool {}
                 :spec {:host "127.0.0.1"
                        :port 6379}})

(def prod? (= (env :environment) "prod"))
(def dev-cache
  "Cache used in place of redis when running in dev/demo mode"
  (atom {}))

(defn cache-set! [k v]
  (if prod?
    (car/wcar redis-conn (car/set k v))
    (swap! dev-cache assoc k v)))

(defn cache-get [k]
  (if prod?
    (car/wcar redis-conn (car/get k))
    (@dev-cache k)))

(defn cache-del! [k]
  (if prod?
    (car/wcar redis-conn (car/del k))
    (swap! dev-cache dissoc k)))

(def redis-conn {:pool {}
                 :spec {:host "127.0.0.1"
                        :port 6379}})

