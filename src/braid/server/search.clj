(ns braid.server.search
  (:require
    [braid.server.db :as db]
    [braid.server.search.elasticsearch :as elastic]
    [clojure.set :refer [intersection]]
    [clojure.string :as string]
    [datomic.api :as d]
    [instaparse.core :as insta]))

; TODO: some way to search for a tag with spaces in it?
(def query-parser
  (insta/parser
    "S ::= ( TAG / DOT ) *
    DOT ::= #'(?s).'
    <ws> ::= #'\\s*'
    TAG ::= <'#'> #'[^ ]+'
    "))

(defn squash
  [s]
  (if (string/blank? s)
   ""
    (-> s
        (string/replace #"\s+" " ")
        (string/replace #"^\s+" ""))))

(defn parse-query
  [txt]
  (let [parsed (query-parser txt)
        text-query (->> parsed
                        (insta/transform
                          {:DOT str
                           :TAG (constantly "")})
                        rest
                        (string/join "")
                        squash)
        tag-query (->> parsed
                       (insta/transform
                         {:DOT (constantly nil)
                          :TAG identity})
                       rest
                       (remove nil?))]
    {:text text-query
     :tags tag-query}))

(defn search-threads-as
  "Return the ids of all threads, visible to the user, in the given group,
  matching the provided query.
  The query can speficy tags by prefixing them with an octothope; for example,
  the query 'foo #bar' will find any threads tagged with 'bar' containing the
  text 'foo'"
  [user-id [query group-id]]
  ; TODO: pagination?
  (let [{:keys [text tags]} (parse-query query)
        search-db (d/db db/conn)
        tag-search (when (seq tags)
                     (set (d/q '[:find ?t-id (max ?time)
                                 :in $ [?tag-name ...] ?g-id
                                 :where
                                 ; TODO: be more flexible/allow partial match?
                                 [?g :group/id ?g-id]
                                 [?tag :tag/group ?g]
                                 [?tag :tag/name ?tag-name]
                                 [?t :thread/tag ?tag]
                                 [?t :thread/id ?t-id]
                                 [?m :message/thread ?t]
                                 [?m :message/created-at ?time]]
                               search-db
                               tags
                               group-id)))
        text-search (when-not (string/blank? text)
                      (if (elastic/elasticsearch-enabled?)
                        (elastic/search-for {:text text
                                             :group-id group-id
                                             :user-id user-id})
                        (set (d/q '[:find ?t-id (max ?time)
                                    :in $ ?txt ?g-id
                                    :where
                                    [?g :group/id ?g-id]
                                    [?tag :tag/group ?g]
                                    [?t :thread/id ?t-id]
                                    [?t :thread/tag ?tag]
                                    [?m :message/thread ?t]
                                    [?m :message/created-at ?time]
                                    [(fulltext $ :message/content ?txt) [[?m]]]]
                                  search-db
                                  text
                                  group-id))))]
    (->> (if (every? some? [text-search tag-search])
           (intersection text-search tag-search)
           (first (remove nil? [text-search tag-search])))
         (filter (comp (partial db/user-can-see-thread? user-id) first))
         ; sorting the ids so we can have a consistent order of results
         (sort-by second #(compare %2 %1))
         (map first))))
