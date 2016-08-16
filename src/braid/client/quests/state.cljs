(ns braid.client.quests.state
  (:require [schema.core :as s :include-macros true]
            [braid.client.quests.list :refer [quests]]))

(def init-state
  ;{:quests {}}
  {:quests
   (->> quests
        (map (fn [quest]
               (assoc quest
                 :state :inactive
                 :progress 0)))
        ; mark first three as active
        (map-indexed (fn [i quest]
                       (if (< i 3)
                         (assoc quest :state :active)
                         quest)))
        (map (fn [quest]
               [(quest :id) quest]))
        (into {}))})

(def QuestId
  s/Keyword)

(def Quest
  {:id QuestId
   :name s/Str
   (s/optional-key :icon) s/Str ;TODO shouldn't be optional
   :state (s/enum :inactive
                  :active
                  :complete
                  :skipped)
   :count s/Int
   :progress s/Int
   :listener js/Function})

(def QuestsState
  {:quests {QuestId Quest}})