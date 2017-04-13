(ns braid.server.db.upload
  (:require
    [datomic.api :as d]
    [braid.server.db :as db]
    [braid.server.db.common :refer [create-entity-txn db->upload
                                    upload-pull-pattern]]))

;; Queries

(defn uploads-in-group
  [group-id]
  (->> (d/q '[:find (pull ?u pull-pattern)
              :in $ ?group-id pull-pattern
              :where
              [?g :group/id ?group-id]
              [?t :thread/group ?g]
              [?u :upload/thread ?t]]
            (db/db) group-id upload-pull-pattern)
       (map (comp db->upload first))
       (sort-by :uploaded-at #(compare %2 %1))))

;; Transactions

(defn create-upload-txn
  [{:keys [id url thread-id uploader-id uploaded-at]}]
  (create-entity-txn
    {:upload/id id
     :upload/url url
     :upload/thread [:thread/id thread-id]
     :upload/uploaded-by [:user/id uploader-id]
     :upload/uploaded-at uploaded-at}
    db->upload))
