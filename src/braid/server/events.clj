(ns braid.server.events
  (:require
    [clavatar.core :refer [gravatar]]
    [braid.server.crypto :refer [random-nonce]]
    [braid.server.db :as db]
    [braid.server.sync-helpers :as sync-helpers]))

(defn add-user-to-recent-threads-in-group!
  [user-id group-id]
  (doseq [t (db/recent-threads {:user-id user-id :group-id group-id
                                :num-threads 5})]
    (db/user-show-thread! user-id (t :id))))

(defn user-join-group!
  [user-id group-id]
  (db/user-add-to-group! user-id group-id)
  (db/user-subscribe-to-group-tags! user-id group-id)
  (add-user-to-recent-threads-in-group! user-id group-id)
  (sync-helpers/broadcast-group-change group-id [:braid.client/new-user (db/user-by-id user-id)]))

(defn register-user!
  [email group-id]
  (let [; TODO: guard against duplicate nickname?
        user (db/create-user! {:id (db/uuid)
                               :email email
                               :password (random-nonce 50)
                               :avatar (gravatar email
                                         :rating :g
                                         :default :identicon)})]
    (user-join-group! (user :id) group-id)
    (user :id)))

