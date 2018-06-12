(ns braid.quests.client.core
  (:require
    [clojure.spec.alpha :as s]
    [re-frame.core :as re-frame]
    [braid.quests.client.events]
    [braid.quests.client.subs]
    [braid.quests.client.helpers :as helpers]
    [braid.quests.client.remote-handlers]))

(defn initial-data-handler
  [db data]
  (helpers/set-quest-records db (data :quest-records)))

(def event-listener
  (fn [event]
    (when (not= "quests" (namespace (first event)))
      (re-frame/dispatch [:quests/update-handler event]))))

(def initial-state {::quest-records {}})
(def schema {::quest-records
             {uuid?
              {:quest-record/id uuid?
               :quest-record/quest-id keyword?
               :quest-record/progress integer?
               :quest-record/state (s/spec #{:inactive
                                             :active
                                             :complete
                                             :skipped})}}})
