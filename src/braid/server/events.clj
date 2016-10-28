(ns braid.server.events
  (:require
    [clavatar.core :refer [gravatar]]
    [braid.server.crypto :refer [random-nonce]]
    [braid.server.db :as db]
    [braid.server.sync :as sync]))

(defn register-user!
  [email group-id]
  (let [id (db/uuid)
        ; TODO: guard against duplicate nickname?
        u (db/create-user! {:id id
                            :email email
                            :password (random-nonce 50)
                            :avatar (gravatar email
                                      :rating :g
                                      :default :identicon)})]
    (sync/user-join-group! id group-id)
    id))

