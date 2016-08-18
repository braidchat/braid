(ns braid.client.dispatcher
  (:require [braid.client.store :as store]
            [braid.client.state.handler.core :refer [handler]]))

(defn dispatch!
  ([event args]
   (println event)
   (store/transact! (handler @store/app-state [event args])))
  ([event]
   (dispatch! event nil)))
