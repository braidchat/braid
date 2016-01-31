(ns chat.server.search
  (:require [datomic.api :as d]
            [clojure.string :as string]
            [clojure.set :refer [intersection]]
            [instaparse.core :as insta]
            [chat.server.db :as db]))

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
  [user-id [query group-id]]
  ; TODO: pagination?
  ; TODO: consistent order for results
  (let [{:keys [text tags]} (parse-query query)
        search-db (d/db db/*conn*)
        tag-search (when (seq tags)
                     (set (d/q '[:find [?t-id ...]
                                 :in $ [?tag-name ...] ?g-id
                                 :where
                                 ; TODO: be more flexible/allow partial match?
                                 [?tag :tag/name ?tag-name]
                                 [?t :thread/tag ?tag]
                                 [?t :thread/id ?t-id]
                                 [?tag :tag/group ?g]
                                 [?g :group/id ?g-id]]
                               search-db
                               tags
                               group-id)))
        text-search (when-not (string/blank? text)
                      (set (d/q '[:find [?t-id ...]
                                  :in $ ?txt ?g-id
                                  :where
                                  [(fulltext $ :message/content ?txt) [[?m]]]
                                  [?m :message/thread ?t]
                                  [?t :thread/id ?t-id]
                                  [?t :thread/tag ?tag]
                                  [?tag :tag/group ?g]
                                  [?g :group/id ?g-id]]
                                search-db
                                text
                                group-id)))]
    (->> (if (every? some? [text-search tag-search])
           (intersection text-search tag-search)
           (first (remove nil? [text-search tag-search])))
         (into #{} (filter (partial db/user-can-see-thread? user-id))))))
