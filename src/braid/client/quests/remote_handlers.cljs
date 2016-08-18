(ns braid.client.quests.remote-handlers
  (:require
    [braid.client.sync :as sync]
    [braid.client.dispatcher :refer [dispatch!]]))

(defmethod sync/event-handler :braid.client.quests/store-quest-record
  [[_ quest-record]]
  (dispatch! :quests/store-quest-record {:quest-record quest-record
                                         :local-only? true}))

(defmethod sync/event-handler :braid.client.quests/skip-quest
  [[_ quest-record-id]]
  (dispatch! :quests/skip-quest {:quest-record-id quest-record-id
                                 :local-only? true}))

(defmethod sync/event-handler :braid.client.quests/increment-quest
  [[_ quest-record-id]]
  (dispatch! :quests/increment-quest {:quest-record-id quest-record-id
                                 :local-only? true}))

(defmethod sync/event-handler :braid.client.quests/complete-quest
  [[_ quest-record-id]]
  (dispatch! :quests/complete-quest {:quest-record-id quest-record-id
                                     :local-only? true}))


