(ns braid.client.quests.state
  (:require [schema.core :as s :include-macros true]
            [braid.client.quests.list :refer [quests]]
            [cljs-uuid-utils.core :as uuid]))

(def init-state
  {:quest-records {}})

(def QuestRecordId
  s/Uuid)

(def QuestId
  s/Keyword)

(def QuestRecord
  {:quest-record/id QuestRecordId
   :quest-record/quest-id QuestId
   :quest-record/state (s/enum :inactive
                  :active
                  :complete
                  :skipped)
   :quest-record/progress s/Int})

(def QuestsState
  {:quest-records {QuestRecordId QuestRecord}})
