(ns braid.client.dispatcher
  (:require [braid.client.store :as store]
            [braid.client.state.handler.core :refer [handler]]
            [braid.client.quests.handler :refer [quests-handler]]))

(defn dispatch!
  ([event args]
   (println event)
   (store/transact! (-> @store/app-state
                        (quests-handler [event args])
                        (handler [event args]))))
  ([event]
   (dispatch! event nil)))
