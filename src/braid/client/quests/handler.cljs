(ns braid.client.quests.handler
  (:require [braid.client.quests.helpers :as helpers]))

(defn quests-handler [state [event args]]
  (let [updated-quests
        (->> (helpers/get-active-quests state)
             (map (fn [quest]
                    (let [inc-progress? ((quest :listener) state [event args])]
                      (if inc-progress?
                        (update quest :progress inc)
                        quest))))
             (map (fn [quest]
                    [(quest :id) quest]))
             (into {}))]

    (update-in state [:quests]
               (fn [quests]
                 (merge quests
                        updated-quests)))))
