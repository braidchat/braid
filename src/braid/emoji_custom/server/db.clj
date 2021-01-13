(ns braid.emoji-custom.server.db
  (:require
   [datomic.api :as d]
   [datomic.db]
   [braid.core.server.db :as db] ; FIXME use of non-public API
   ))

(def db-schema
  [{:db/ident :custom-emoji/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :custom-emoji/shortcode
    :db/doc "Shortcode to type the emoji - e.g. code `foo` means one can type `:foo:` to get the emoji"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :custom-emoji/image
    :db/doc "URL for the image to display for said emoji"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :custom-emoji/group
    :db/doc "The group the emoji is registered in"
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def emoji-pull-pattern
  [:custom-emoji/id
   :custom-emoji/shortcode
   :custom-emoji/image
   {:custom-emoji/group [:group/id]}])

(defn db->emoji
  [e]
  {:id (:custom-emoji/id e)
   :group-id (get-in e [:custom-emoji/group :group/id])
   :shortcode (:custom-emoji/shortcode e)
   :image (:custom-emoji/image e)})

(defn emoji-by-id
  [id]
  (db->emoji (d/pull (db/db) emoji-pull-pattern [:custom-emoji/id id])))

(defn group-custom-emoji
  [group-id]
  (->> (d/q '[:find [(pull ?e pattern) ...]
             :in $ pattern ?group-id
             :where
             [?e :custom-emoji/group ?g]
             [?g :group/id ?group-id]]
           (db/db) emoji-pull-pattern group-id)
      (map db->emoji)))

(defn emoji-group
  [emoji-id]
  (-> (d/pull (db/db) [{:custom-emoji/group [:group/id]}]
              [:custom-emoji/id emoji-id])
      (get-in [:custom-emoji/group :group/id])))

(defn add-custom-emoji-txn
  [{:keys [id group-id shortcode image]}]
  [{:db/id (d/tempid :entities)
    :custom-emoji/id id
    :custom-emoji/group [:group/id group-id]
    :custom-emoji/shortcode (str ":" shortcode ":")
    :custom-emoji/image image}])

(defn edit-custom-emoji-txn
  [id shortcode]
  [[:db/add [:custom-emoji/id id]
    :custom-emoji/shortcode (str ":" shortcode ":")]])

(defn retract-custom-emoji-txn
  [emoji-id]
  [[:db.fn/retractEntity [:custom-emoji/id emoji-id]]])
