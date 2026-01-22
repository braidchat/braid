(ns braid.delete-user
  (:require
    [datomic.api :as d]
    [braid.core.server.db :as db]))

(defn find-user-content [email]
  (let [user-eid (d/q '[:find ?u .
                        :in $ ?email
                        :where
                        [?u :user/email ?email]]
                      (db/db)
                      email)
        message-eids (d/q '[:find [?m ...]
                            :in $ ?u
                            :where
                            [?m :message/user ?u]]
                          (db/db)
                          user-eid)
        thread-eids (d/q '[:find [?t ...]
                           :in $ ?u
                           :where
                           [?m :message/user ?u]
                           [?m :message/thread ?t]]
                         (db/db)
                         user-eid)
        preference-eids (d/q '[:find [?p ...]
                               :in $ ?u
                               :where
                               [?u :user/preferences ?p]]
                             (db/db)
                             user-eid)]
    {:user [user-eid]
     :messages message-eids
     :threads thread-eids
     :preferences preference-eids}))

(defn delete-user! [email]
  (->> (find-user-content email)
       (mapcat val)
       (map (fn [eid]
              {:db/excise eid}))
       (d/transact db/conn)))

#_(delete-user! "foo@example.com")

(defn check [email]
  (d/q '[:find ?u .
         :in $ ?email
         :where
         [?u :user/email ?email]]
       (db/db)
       email))

;; all schema
#_(->> (d/q '[:find ?v
              :where [_ :db.install/attribute ?v]]
            (db/db))
       (map #(->> % first (d/entity (db/db)) d/touch))
       (map :db/ident))
