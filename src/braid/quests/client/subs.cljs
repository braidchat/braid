(ns braid.quests.client.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [braid.quests.client.helpers :as helpers]))

(reg-sub
  :quests/active-quest-records
  (fn [state _]
    (helpers/get-active-quest-records state)))
