(ns braid.server.events
  (:require
    [clojure.string :as string]
    [clavatar.core :refer [gravatar]]
    [braid.server.crypto :refer [random-nonce]]
    [braid.server.db :as db]
    [braid.server.sync :as sync]))

(defn register-user!
  [email group-id]
  (let [id (db/uuid)
        ; XXX: copied from braid.common.util/nickname-rd
        disallowed-chars #"[ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]"
        nick (-> (first (string/split email #"@"))
                 (string/replace disallowed-chars ""))
        ; TODO: guard against duplicate nickname?
        u (db/create-user! {:id id
                            :email email
                            :password (random-nonce 50)
                            :avatar (gravatar email
                                      :rating :g
                                      :default :identicon)
                            :nickname nick})]
    (sync/user-join-group! id group-id)
    id))

