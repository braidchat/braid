(ns braid.search.lucene
  (:require
   [braid.core.server.conf :refer [config]]
   [clucie.analysis :as analysis]
   [clucie.core :as clucie]
   [clucie.store :as store]
   [clucie.query :as query]
   [clojure.string :as string]
   [datomic.api :as d]
   [braid.core.server.db :as db]
   [braid.core.server.db.common :as common])
  (:import
   (org.apache.lucene.index IndexNotFoundException)
   (org.apache.lucene.analysis.core LowerCaseFilter)
   (org.apache.lucene.analysis.icu ICUNormalizer2CharFilterFactory)
   (org.apache.lucene.analysis.standard StandardTokenizer)))

(extend-protocol query/FormParsable
  java.util.UUID
  (parse-formt [u opts]
    (query/parse-formt (str u) opts)))

(def analyzer
  (analysis/build-analyzer
    (StandardTokenizer.)
    :char-filter-factories [(ICUNormalizer2CharFilterFactory.
                              (java.util.HashMap. {"name" "nfkc"
                                                   "mode" "compose"}))]
    :token-filters [(LowerCaseFilter.)]))

(defonce -store (atom nil))
(defn store
  []
  (if-let [s @-store]
    s
    (let [s (if-let [store-path (config :lucene-store-location)]
              (store/disk-store store-path)
              (store/memory-store))]
      (reset! -store s)
      s)))

(defn index-message!
  ([message]
   (with-open [writer (store/store-writer (store) analyzer)]
     (index-message! writer (store) message)))
  ([writer reader message]
   (let [message (update message :created-at (memfn getTime))]
     (if-let [existing (try
                         (first
                           (clucie/search reader {:group-id (:group-id message)
                                                  :thread-id (:thread-id message)}
                                          1))
                           (catch IndexNotFoundException _
                             nil))]
       (clucie/update!
         writer
         (-> existing
             (update :content str "\n" (:content message))
             (update :created-at #(max (:created-at message) (Long. %))))
         [:group-id :thread-id :content]
         :thread-id (:thread-id message))
       (clucie/add!
         writer
         [message]
         [:group-id :thread-id :content #_:mentioned-tag-ids #_:mentioned-user-ids])))))

(defn search
  ([group-id text] (search (store) group-id text))
  ([reader group-id text]
   (->> (clucie/search
         reader
         {:group-id group-id
          :content (string/split text #"\s+")}
         1000
         analyzer
         0
         100)
       (map (fn [{:keys [thread-id created-at] :as res}]
              {:thread-id (java.util.UUID/fromString thread-id)
               :created-at (Long. created-at)}))
       (group-by :thread-id)
       (into #{}
             (map (fn [[thread-id threads]]
                    [thread-id (java.util.Date. (apply max (map :created-at threads)))]))))))

(defn import-messages!
  []
  (index-message! {:content "hello" :group-id (java.util.UUID/randomUUID)
                   :thread-id (java.util.UUID/randomUUID) :created-at (java.util.Date.)})
  (with-open [reader (store/store-reader (store))
              writer (store/store-writer (store) analyzer)]
    (doseq [msg (->> (d/q '[:find [(pull ?m pull-pattern) ...]
                           :in $ pull-pattern
                           :where [?m :message/id _]]
                         (db/db)
                         common/message-pull-pattern)
                    (map common/db->message))]
      (when (and (:group-id msg) (:thread-id msg) (:content msg)
                 (:created-at msg))
        (index-message! writer reader msg)))))
