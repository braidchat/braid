(ns braid.search.lucene
  (:require
   [braid.core.server.conf :refer [config]]
   [clucie.analysis :as analysis]
   [clucie.core :as clucie]
   [clucie.store :as store]
   [clucie.query :as query]
   [clojure.string :as string])
  (:import
   (org.apache.lucene.analysis.core LowerCaseFilter)
   (org.apache.lucene.analysis.icu ICUNormalizer2CharFilterFactory)
   (org.apache.lucene.analysis.standard StandardTokenizer)
   ))

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
  [message]
  (prn "INDEXING" message)
  (clucie/add!
    (store)
    [(update message :created-at (memfn getTime))]
    [:group-id :content #_:mentioned-tag-ids #_:mentioned-user-ids]
    analyzer))

(defn search
  [group-id text]
  (->> (clucie/search
        (store)
        {:group-id group-id
         :content (string/split text #"\s+")}
        1000
        analyzer
        0
        100)
      (map (fn [{:keys [thread-id created-at] :as res}]
             (prn "search res" res)
             {:thread-id (java.util.UUID/fromString thread-id)
              :created-at (java.util.Date. (Long. created-at))}))
      (group-by :thread-id)
      (into #{}
            (map (fn [[thread-id threads]]
                   (prn "CREATED ATS" (map :created-at threads))
                   [thread-id (apply max (map :created-at threads))])))))
