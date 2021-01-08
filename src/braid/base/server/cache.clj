(ns braid.base.server.cache
  (:require
   [braid.base.conf :as conf]
   [taoensso.carmine :as car]))

; same as conf in handler, but w/e
(def redis-conn (delay
                  {:pool {}
                   :spec {:uri (conf/config :redis-uri)}}))

(def redis? (delay (conf/config :redis-uri)))

(def dev-cache
  "Cache used in place of redis when running in dev/demo mode"
  (atom {}))

(defn cache-set! [k v]
  (if redis?
    (car/wcar @redis-conn (car/set k v))
    (swap! dev-cache assoc k v)))

(defn cache-get [k]
  (if @redis?
    (car/wcar @redis-conn (car/get k))
    (@dev-cache k)))

(defn cache-del! [k]
  (if @redis?
    (car/wcar @redis-conn (car/del k))
    (swap! dev-cache dissoc k)))
