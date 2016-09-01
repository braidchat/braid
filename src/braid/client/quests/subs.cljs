(ns braid.client.quests.subs
  (:require [re-frame.core :refer [reg-sub]]
            [braid.client.quests.helpers :as helpers]))

(reg-sub
  :quests/active-quest-records
  (fn [state _]
    (helpers/get-active-quest-records state)))
