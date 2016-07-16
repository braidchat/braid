(ns braid.server.db.upload
  (:require [datomic.api :as d]
            [braid.server.db.common :refer :all]))

(defn create-upload!
  [conn {:keys [id url thread-id uploader-id uploaded-at]}]
  (->> {:upload/id id
        :upload/url url
        :upload/thread [:thread/id thread-id]
        :upload/uploaded-by [:user/id uploader-id]
        :upload/uploaded-at uploaded-at}
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
       (sort-by :uploaded-at #(compare %2 %1))))
