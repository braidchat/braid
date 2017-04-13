(ns braid.client.quests.schema
  (:require
    [cljs-uuid-utils.core :as uuid]
    [schema.core :as s :include-macros true]
    [braid.common.schema :refer [QuestRecordId QuestRecord]]))

(def init-state
  {:quest-records {}})

(def QuestsAppState
  {:quest-records {QuestRecordId QuestRecord}})
