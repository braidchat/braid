(ns braid.server.events
  (:require
    [clojure.string :as string]
    [braid.server.crypto :refer [random-nonce]]
    [braid.server.db :as db]
    [braid.server.sync :as sync]
    [braid.server.identicons :as identicons]))

(defn register-user!
  [email group-id]
  (let [id (db/uuid)
        avatar (identicons/id->identicon-data-url id)
        ; XXX: copied from braid.common.util/nickname-rd
        disallowed-chars #"[ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]"
        nick (-> (first (string/split email #"@"))
                 (string/replace disallowed-chars ""))
        ; TODO: guard against duplicate nickname?
        u (db/create-user! {:id id
                            :email email
                            :password (random-nonce 50)
                            :avatar avatar
                            :nickname nick})]
    (sync/user-join-group! id group-id)
    id))

