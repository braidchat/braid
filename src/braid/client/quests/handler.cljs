(ns braid.client.quests.handler
  (:require [braid.client.quests.helpers :as helpers]
            [braid.client.quests.list :refer [quests-by-id]]))

(defn quests-handler [state [event args]]
  (let [updated-quest-records
        (->> (helpers/get-active-quest-records state)
             (map (fn [quest-record]
                    (let [quest (quests-by-id (quest-record :quest-id))
                          inc-progress? ((quest :listener) state [event args])]
                      (if inc-progress?
                        (update quest-record :progress inc)
                        quest-record))))
             (map (fn [quest-record]
                    [(quest-record :id) quest-record]))
             (into {}))]

    ; TODO persist progress to backend

    (update-in state [:quest-records]
               (fn [quest-records]
                 (merge quest-records
                        updated-quest-records)))))
