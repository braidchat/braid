(ns braid.emoji-custom.client.state
  (:require
    [clojure.set :as set]
    [re-frame.core :refer [dispatch]]))

(defn- key-by [k coll]
  (reduce (fn [memo i]
            (assoc memo (k i) i)) {} coll))

(defn initial-data-handler
  [db data]
  (assoc db :custom-emoji/group-emoji
    (->> (data :custom-emoji/custom-emoji)
         (map (fn [[group-id emojis]]
                [group-id (key-by :shortcode emojis)]))
         (into {}))))

(def initial-state {:custom-emoji/group-emoji {}})

(def state-spec {:custom-emoji/group-emoji any?})

(def subscriptions
  {:custom-emoji/group-emojis
   (fn [db [_ group-id]]
     (vals (get-in db [:custom-emoji/group-emoji
                       (or group-id (db :open-group-id))])))

   :custom-emoji/custom-emoji
   (fn [db [_ shortcode]]
     (get-in db [:custom-emoji/group-emoji
                 (db :open-group-id)
                 shortcode]))})

(def events
  {:custom-emoji/add-emoji
   (fn [_ [_ {:keys [group-id shortcode image] :as emoji}]]
     {:websocket-send
      (list [:braid.server.custom-emoji/add-custom-emoji emoji])})

   :custom-emoji/edit-emoji
   (fn [_ [_ emoji-id new-code]]
     {:websocket-send
      (list [:braid.server.custom-emoji/edit-custom-emoji [emoji-id new-code]])})

   :custom-emoji/retract-emoji
   (fn [_ [_ emoji-id]]
     {:websocket-send
      (list [:braid.server.custom-emoji/retract-custom-emoji emoji-id])})

   :custom-emoji/new-emoji-notification
   (fn [{db :db} [_ new-emoji]]
     {:db (assoc-in db [:custom-emoji/group-emoji
                        (new-emoji :group-id)
                        (new-emoji :shortcode)]
            new-emoji)})

   :custom-emoji/edit-emoji-notification
   (fn [{db :db} [_ group-id old-code new-code]]
     {:db (-> (update-in db [:custom-emoji/group-emoji group-id]
                         set/rename-keys {old-code new-code})
              (assoc-in [:custom-emoji/group-emoji group-id
                         new-code :shortcode] new-code))})

   :custom-emoji/remove-emoji-notification
   (fn [{db :db} [_ group-id emoji-id]]
     {:db (update-in
            db
            [:custom-emoji/group-emoji group-id]
            (partial into {}
                     (remove (fn [[_ {id :id}]] (= id emoji-id)))))})})


(def socket-message-handlers
  {:custom-emoji/new-emoji-notification
   (fn [_ new-emoji]
     (dispatch [:custom-emoji/new-emoji-notification new-emoji]))

   :custom-emoji/remove-emoji-notification
   (fn [_ [group-id emoji-id]]
     (dispatch [:custom-emoji/remove-emoji-notification group-id emoji-id]))

   :custom-emoji/edit-emoji-notification
   (fn [_ [group-id old-code new-code]]
     (dispatch [:custom-emoji/edit-emoji-notification group-id old-code new-code]))})
