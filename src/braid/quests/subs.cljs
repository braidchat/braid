(ns braid.quests.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [braid.quests.helpers :as helpers]))

(reg-sub
  :quests/active-quest-records
  (fn [state _]
    (helpers/get-active-quest-records state)))
