(ns braid.client.quests.handler
  (:require [braid.client.quests.helpers :as helpers]
            [braid.client.quests.list :refer [quests-by-id]]))

(defn maybe-increment-quest-record [state quest-record event args]
  (let [quest (quests-by-id (quest-record :quest-record/quest-id))
        inc-progress? ((quest :quest/listener) state [event args])
        local-only? (:local-only? args)]
    (when (and inc-progress? (not local-only?))
      [:quests/increment-quest (quest-record :quest-record/id)])))

(defn quests-handler [state [event args]]
  {:dispatch-n
   (into ()
         (comp (map
                 (fn [quest-record]
                   (maybe-increment-quest-record state quest-record event args)))
               (remove nil?))
         (helpers/get-active-quest-records state))})

