(ns braid.custom-emoji.server.core
  (:require
   [braid.chat.db.group :as group-db] ;FIXME use of non-public API
   [braid.custom-emoji.server.db :as emoji-db]))

(defn initial-user-data-fn
  [user-id]
  {:custom-emoji/custom-emoji (into {}
                                    (map (fn [{group-id :id}]
                                           [group-id (emoji-db/group-custom-emoji group-id)]))
                                    (group-db/user-groups user-id))})

(def server-message-handlers
  {:braid.server.custom-emoji/add-custom-emoji
   (fn [{user-id :user-id {:keys [group-id shortcode image] :as data} :?data}]
     (when (group-db/user-is-group-admin? user-id group-id)
       (let [new-emoji (assoc data :id (java.util.UUID/randomUUID))]
         {:db-run-txns! (emoji-db/add-custom-emoji-txn new-emoji)
          :group-broadcast! [group-id
                             [:custom-emoji/new-emoji-notification
                              (-> new-emoji
                                  (assoc :group-id group-id)
                                  (update :shortcode #(str ":" % ":")))]]})))

   :braid.server.custom-emoji/retract-custom-emoji
   (fn [{user-id :user-id emoji-id :?data}]
     (let [group-id (emoji-db/emoji-group emoji-id)]
       (when (group-db/user-is-group-admin? user-id group-id)
         {:db-run-txns! (emoji-db/retract-custom-emoji-txn emoji-id)
          :group-broadcast! [group-id [:custom-emoji/remove-emoji-notification
                                       [group-id emoji-id]]]})))

   :braid.server.custom-emoji/edit-custom-emoji
   (fn [{user-id :user-id [emoji-id new-code] :?data}]
     (let [old-emoji (emoji-db/emoji-by-id emoji-id)]
       (when (group-db/user-is-group-admin? user-id (:group-id old-emoji))
         {:db-run-txns! (emoji-db/edit-custom-emoji-txn emoji-id new-code)
          :group-broadcast! [(old-emoji :group-id)
                             [:custom-emoji/edit-emoji-notification
                              [(old-emoji :group-id)
                               (old-emoji :shortcode)
                               (str ":" new-code ":")]]]})))})
