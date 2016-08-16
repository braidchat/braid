(ns braid.client.quests.handlers
  (:require [braid.client.state.handler.core :refer [handler]]))

(defmethod handler :skip-quest [state [_ quest-id]]
  (update-in state [:user :completed-quest-ids]
             (fn [s]
               (conj s quest-id))))
