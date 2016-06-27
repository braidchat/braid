(ns braid.server.db.upload
  (:require [datomic.api :as d]
            [braid.server.db.common :refer :all]))

(defn create-upload!
  [conn {:keys [id url thread-id]}]
  (->> {:upload/id id
        :upload/url url
        :upload/thread [:thread/id thread-id]
        :upload/uploaded-at (java.util.Date.)}
       (create-entity! conn)
       db->upload))

(defn uploads-in-group
  [conn group-id]
  (->> (d/q '[:find (pull ?u pull-pattern)
              :in $ ?group-id pull-pattern
              :where
              [?g :group/id ?group-id]
              [?t :thread/group ?g]
              [?u :upload/thread ?t]]
            (d/db conn) group-id upload-pull-pattern)
       (map (comp db->upload first))
       (sort-by :uploaded-at)))
