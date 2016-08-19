(ns braid.client.quests.subscriptions
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.state.subscription :refer [subscription]]
            [braid.client.quests.helpers :as helpers]))

(defmethod subscription :quests/active-quest-records
  [state _]
  (reaction (helpers/get-active-quest-records @state)))
