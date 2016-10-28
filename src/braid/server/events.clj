(ns braid.server.events
  (:require
    [clavatar.core :refer [gravatar]]
    [braid.server.crypto :refer [random-nonce]]
    [braid.server.db :as db]
    [braid.server.sync :as sync]))

(defn register-user!
  [email group-id]
  (let [; TODO: guard against duplicate nickname?
        user (db/create-user! {:id (db/uuid)
                               :email email
                               :password (random-nonce 50)
                               :avatar (gravatar email
                                         :rating :g
                                         :default :identicon)})]
    (sync/user-join-group! (user :id) group-id)
    (user :id)))

