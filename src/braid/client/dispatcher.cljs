(ns braid.client.dispatcher
  (:require [braid.client.store :as store]
            [braid.client.state.handler.core :refer [handler]]
            [braid.client.state.handler.quests :refer [quests-handler]]))

(defn dispatch!
  ([event args]
   (println event)
   (store/transact! (-> @store/app-state
                        (handler [event args])
                        (quests-handler [event args]))))
  ([event]
   (dispatch! event nil)))
