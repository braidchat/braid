(ns braid.emoji.server.core
  (:require
   [braid.core.server.db.group :as group-db]
   [braid.emoji.server.db :as emoji-db]))

(defn initial-user-data-fn
  [user-id]
  {:emoji/custom-emoji (into {}
                             (map (fn [{group-id :id}]
                                    [group-id (emoji-db/group-custom-emoji group-id)]))
                             (group-db/user-groups user-id))})

(def server-message-handlers
  {:braid.server.emoji/add-custom-emoji
   (fn [{user-id :user-id {:keys [group-id shortcode image] :as data} :?data}]
     (when (group-db/user-is-group-admin? user-id group-id)
       (let [new-emoji (assoc data :id (java.util.UUID/randomUUID))]
         {:db-run-txns! (emoji-db/add-custom-emoji-txn new-emoji)
          :group-broadcast! [group-id [:braid.emoji/new-emoji-notification
                                       (assoc new-emoji :group-id group-id)]]})))})
