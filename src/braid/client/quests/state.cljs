(ns braid.client.quests.state
  (:require [schema.core :as s :include-macros true]
            [braid.client.quests.list :refer [quests]]
            [cljs-uuid-utils.core :as uuid]))

(def init-state
  #_{:quest-records {}}
  {:quest-records
   (->> quests
        (take 3)
        (map (fn [quest]
               {:id (uuid/make-random-squuid)
                :quest-id (quest :id)
                :state :active
                :progress 0}))
        (map (fn [quest-record]
               [(quest-record :id) quest-record]))
        (into {}))})

(def QuestRecordId
  s/Uuid)

(def QuestId
  s/Keyword)

(def QuestRecord
  {:id QuestRecordId
   :quest-id QuestId
   :state (s/enum :inactive
                  :active
                  :complete
                  :skipped)
   :progress s/Int})

(def QuestsState
  {:quest-records {QuestRecordId QuestRecord}})
