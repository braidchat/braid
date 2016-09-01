(ns braid.client.quests.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [cljs-uuid-utils.core :as uuid]
            [braid.client.quests.helpers :as helpers]
            [braid.client.quests.list :refer [quests-by-id]]))

(defn make-quest-record [{:keys [quest-id]}]
  {:quest-record/id (uuid/make-random-squuid)
   :quest-record/state :active
   :quest-record/progress 0
   :quest-record/quest-id quest-id})

(reg-event-fx
  :quests/activate-next-quest
  (fn [{state :db :as cofx} _]
    (if-let [quest (helpers/get-next-quest state)]
      (let [quest-record (make-quest-record {:quest-id (quest :quest/id)})
            state (helpers/store-quest-record state quest-record)]
        {:db state
         :websocket-send (list [:braid.server.quests/upsert-quest-record quest-record])})
      {})))

(reg-event-fx
  :quests/skip-quest
  (fn [state [_ quest-record-id]]
    (let [state (helpers/skip-quest state quest-record-id)]
      {:websocket-send
       (list [:braid.server.quests/upsert-quest-record
              (helpers/get-quest-record state quest-record-id)])
       :dispatch [:activate-next-quest]})))

(reg-event-fx
  :quests/complete-quest
  (fn [{state :db :as cofx} [_ quest-record-id]]
    (let [state (helpers/complete-quest state quest-record-id)]
      {:websocket-send
       (list [:braid.server.quests/upsert-quest-record
              (helpers/get-quest-record state quest-record-id)])
       :dispatch [:activate-next-quest]})))

(reg-event-fx
  :quests/increment-quest
  (fn [{state :db :as cofx} [_ quest-record-id]]
    (let [state (helpers/increment-quest state quest-record-id)]
      {:db state
       :websocket-send (list [:braid.server.quests/upsert-quest-record
                              (helpers/get-quest-record state quest-record-id)])})))

(reg-event-db
  :quests/upsert-quest-record
  (fn [state [_ quest-record]]
    (helpers/store-quest-record state quest-record)))

(defn maybe-increment-quest-record [state quest-record event args]
  (let [quest (quests-by-id (quest-record :quest-record/quest-id))
        inc-progress? ((quest :quest/listener) state [event args])
        local-only? (:local-only? args)]
    (when (and inc-progress? (not local-only?))
      [:quests/increment-quest (quest-record :quest-record/id)])))

(reg-event-fx
  :quests/update-handler
  (fn [{state :db :as cofx} [_ [event args]]]
    {:dispatch-n
     (into ()
           (comp (map
                   (fn [quest-record]
                     (maybe-increment-quest-record state quest-record event args)))
                 (remove nil?))
           (helpers/get-active-quest-records state))}))
