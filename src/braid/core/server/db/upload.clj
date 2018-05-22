(ns braid.core.server.db.upload
  (:require
   [datomic.api :as d]
   [braid.core.server.db :as db]
   [braid.core.server.db.common :refer [create-entity-txn db->upload upload-pull-pattern]]))

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

(defn upload-info
  [upload-id]
  (->> (d/pull (db/db)
              [{:upload/uploaded-by [:user/id]}
               {:upload/thread [{:thread/group [:group/id]}]}
               :upload/url]
              [:upload/id upload-id])
      ((fn [up]
         {:id upload-id
          :group-id (get-in up [:upload/thread :thread/group :group/id])
          :user-id (get-in up [:upload/uploaded-by :user/id])
          :url (:upload/url up)}))))

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

(defn retract-upload-txn
  [upload-id]
  [[:db.fn/retractEntity [:upload/id upload-id]]])
