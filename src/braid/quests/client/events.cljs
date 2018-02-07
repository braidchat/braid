(ns braid.quests.client.events
  (:require
    [cljs-uuid-utils.core :as uuid]
    [re-frame.core :refer [reg-event-fx]]
    [braid.quests.client.helpers :as helpers]
    [braid.quests.client.list :refer [quests-by-id]]))

(defn make-quest-record [{:keys [quest-id]}]
  {:quest-record/id (uuid/make-random-squuid)
   :quest-record/state :active
   :quest-record/progress 0
   :quest-record/quest-id quest-id})

(reg-event-fx
  :quests/activate-next-quest
  (fn [{db :db} _]
    (if-let [quest (helpers/get-next-quest db)]
      (let [quest-record (make-quest-record {:quest-id (quest :quest/id)})]
        {:db (helpers/store-quest-record db quest-record)
         :websocket-send (list [:braid.server.quests/upsert-quest-record quest-record])})
      {})))

(reg-event-fx
  :quests/skip-quest
  (fn [{db :db} [_ quest-record-id]]
    (let [db (helpers/skip-quest db quest-record-id)]
      {:db db
       :websocket-send
       (list [:braid.server.quests/upsert-quest-record
              (helpers/get-quest-record db quest-record-id)])
       :dispatch [:quests/activate-next-quest]})))

(reg-event-fx
  :quests/complete-quest
  (fn [{db :db} [_ quest-record-id]]
    (let [db (helpers/complete-quest db quest-record-id)]
      {:db db
       :websocket-send
       (list [:braid.server.quests/upsert-quest-record
              (helpers/get-quest-record db quest-record-id)])
       :dispatch [:quests/activate-next-quest]})))

(reg-event-fx
  :quests/increment-quest
  (fn [{db :db} [_ quest-record-id]]
    (let [db (helpers/increment-quest db quest-record-id)]
      {:db db
       :websocket-send (list [:braid.server.quests/upsert-quest-record
                              (helpers/get-quest-record db quest-record-id)])})))

(reg-event-fx
  :quests/upsert-quest-record
  (fn [{db :db} [_ quest-record]]
    {:db (helpers/store-quest-record db quest-record)}))

(defn maybe-increment-quest-record [db quest-record event args]
  (let [quest (quests-by-id (quest-record :quest-record/quest-id))
        inc-progress? ((quest :quest/listener) db [event args])
        local-only? (:local-only? args)]
    (when (and inc-progress? (not local-only?))
      [:quests/increment-quest (quest-record :quest-record/id)])))

(reg-event-fx
  :quests/update-handler
  (fn [{db :db} [_ [event args]]]
    {:dispatch-n
     (into ()
           (comp (map
                   (fn [quest-record]
                     (maybe-increment-quest-record db quest-record event args)))
                 (remove nil?))
           (helpers/get-active-quest-records db))}))
