(ns braid.client.quests.handlers
  (:require [braid.client.state.handler.core :refer [handler]]
            [braid.client.quests.helpers :as helpers]))

(defmethod handler :quests/skip-quest [state [_ quest-id]]
  (-> state
      (helpers/skip-quest quest-id)
      (helpers/activate-next-quest)))

(defmethod handler :quests/show-quest-instructions [state [_ quest-id]]
  state)

(defmethod handler :quests/get-next-quest [state [_ quest-id]]
  (-> state
      (helpers/complete-quest quest-id)
      (helpers/activate-next-quest)))
