(ns braid.client.quests.remote-handlers
  (:require
    [braid.client.sync :as sync]
    [braid.client.dispatcher :refer [dispatch!]]))

(defmethod sync/event-handler :braid.client.quests/upsert-quest-record
  [[_ quest-record]]
  (dispatch! :quests/upsert-quest-record quest-record))


