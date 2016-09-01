(ns braid.client.quests.schema
  (:require [schema.core :as s :include-macros true]
            [braid.common.schema :refer [QuestRecordId QuestRecord]]
            [cljs-uuid-utils.core :as uuid]))

(def init-state
  {:quest-records {}})

(def QuestsAppState
  {:quest-records {QuestRecordId QuestRecord}})
