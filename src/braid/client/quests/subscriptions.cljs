(ns braid.client.quests.subscriptions
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.state.subscription :refer [subscription]]))

(defmethod subscription :quests/active-quests
  [state _]
  (reaction (->> @state
                :quests
                vals
                (filter (fn [quest]
                          (= (quest :state) :active))))))
