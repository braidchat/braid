(ns braid.chat.server.initial-data
  (:require
    [braid.base.conf :as conf]
    [braid.base.server.socket :refer [connected-uids]]
    [braid.chat.db.group :as group]
    [braid.chat.db.invitation :as invitation]
    [braid.chat.db.tag :as tag]
    [braid.chat.db.thread :as thread]
    [braid.chat.db.user :as user]
    [braid.lib.digest :as digest]))

(defn initial-data-for-user
  [user-id]
  (let [connected (set (:any @connected-uids))
        user-status (fn [user] (if (connected (user :id)) :online :offline))
        update-user-statuses (fn [users]
                               (reduce-kv
                                 (fn [m id u]
                                   (assoc m id (assoc u :status (user-status u))))
                                 {} users))]
    {;; TODO could be part of braid.base
     :user-id user-id
     ;; TODO could be part of braid.base:
     :version-checksum (if (boolean (conf/config :prod-js))
                         (digest/from-file "public/js/prod/desktop.js")
                         (digest/from-file "public/js/dev/desktop.js"))
     :user-groups
     (->> (group/user-groups user-id)
          (map (fn [group] (update group :users update-user-statuses)))
          (map (fn [group]
                 (->> (group/group-users-joined-at (:id group))
                      (reduce (fn [group [user-id joined-at]]
                                (assoc-in
                                  group
                                  [:users user-id :joined-at]
                                  joined-at))
                              group)))))
     :user-threads (thread/open-threads-for-user user-id)
     :user-subscribed-tag-ids (tag/subscribed-tag-ids-for-user user-id)
     :user-preferences (user/user-get-preferences user-id)
     :invitations (invitation/invites-for-user user-id)
     :tags (tag/tags-for-user user-id)}))
