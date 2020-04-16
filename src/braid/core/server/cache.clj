(ns braid.core.server.cache
  (:require
   [environ.core :refer [env]]
   [taoensso.carmine :as car]))

; same as conf in handler, but w/e
(def redis-conn {:pool {}
                 :spec {:uri (env :redis-uri)}})

(def redis? (env :redis-uri))

(def dev-cache
  "Cache used in place of redis when running in dev/demo mode"
  (atom {}))

(defn cache-set! [k v]
  (if redis?
    (car/wcar redis-conn (car/set k v))
    (swap! dev-cache assoc k v)))

(defn cache-get [k]
  (if redis?
    (car/wcar redis-conn (car/get k))
    (@dev-cache k)))

(defn cache-del! [k]
  (if redis?
    (car/wcar redis-conn (car/del k))
    (swap! dev-cache dissoc k)))

(def redis-conn {:pool {}
                 :spec {:host "127.0.0.1"
                        :port 6379}})
