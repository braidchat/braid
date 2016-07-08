(ns braid.server.db.invitation
  (:require [datomic.api :as d]
            [braid.server.db.common :refer :all]))

(defn create-invitation!
  [conn {:keys [id inviter-id invitee-email group-id]}]
  (->> {:invite/id id
        :invite/group [:group/id group-id]
        :invite/from [:user/id inviter-id]
        :invite/to invitee-email
        :invite/created-at (java.util.Date.)}
       (create-entity! conn)
       db->invitation))

(defn invite-by-id
  [conn invite-id]
  (some-> (d/pull (d/db conn)
                  [:invite/id
                   {:invite/from [:user/id :user/email :user/nickname]}
                   :invite/to
                   {:invite/group [:group/id :group/name]}]
                  [:invite/id invite-id])
          db->invitation))

(defn retract-invitation!
  [conn invite-id]
  @(d/transact conn [[:db.fn/retractEntity [:invite/id invite-id]]]))

(defn invites-for-user
  [conn user-id]
  (->> (d/q '[:find (pull ?i [{:invite/group [:group/id :group/name]}
                              {:invite/from [:user/id :user/email :user/nickname]}
                              :invite/to
                              :invite/id])
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?u :user/email ?email]
              [?i :invite/to ?email]]
            (d/db conn) user-id)
       (map (comp db->invitation first))))

