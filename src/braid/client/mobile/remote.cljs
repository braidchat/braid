(ns braid.client.mobile.remote
  (:require [cljs-uuid-utils.core :as uuid]))

(defn key-by-id [arr]
  (reduce (fn [memo i] (assoc memo (:id i) i)) {} arr))

(def fake-response
  (let [group-1-id (uuid/make-random-squuid)
        group-2-id (uuid/make-random-squuid)
        group-3-id (uuid/make-random-squuid)]
  {:user {}
   :groups (key-by-id
             [{:id group-1-id
               :name "Foo"}
              {:id group-2-id
               :name "Bar"}
              {:id group-3-id
               :name "Baz"}])
   :users {44 {:id 44
               :name "therealbatman"}
           55 {:id 55
               :name "brucewayne"}}
   :tags {444 {:id 444
               :name "watercooler"
               :group-id group-1-id}
          555 {:id 555
               :name "braid"
               :group-id group-2-id}}
   :open-group-id group-1-id
   :threads {4 {:id 4
                :group-id group-1-id
                :tag-ids [444 555]
                :messages [{:id 400
                            :content "Alright! it's time to test some messages out."
                            :user-id 44}
                           {:id 401
                            :content "Reply to this please."
                            :user-id 44}
                           {:id 402
                            :content "OK"
                            :user-id 55}
                           {:id 403
                            :content "And now for some junk to pad the message length:"
                            :user-id 44}
                           {:id 404
                            :content "1"
                            :user-id 44}
                           {:id 405
                            :content "2"
                            :user-id 44}
                           {:id 406
                            :content "3"
                            :user-id 55}
                           {:id 407
                            :content "4"
                            :user-id 44}
                           {:id 408
                            :content "5"
                            :user-id 55}
                           {:id 409
                            :content "6"
                            :user-id 44}
                           ]}
             5 {:id 5
                :group-id group-1-id
                :tag-ids [444]
                :messages [{:id 501
                            :content "aaa"
                            :user-id 55}
                           {:id 502
                            :content "bbb"
                            :user-id 44}
                           {:id 503
                            :content "ccc"
                            :user-id 55}]}
             6 {:id 6
                :group-id group-2-id
                :messages [{:id 504
                            :content "xoo"
                            :user-id 44}
                           {:id 505
                            :content "xar"
                            :user-id 55}
                           {:id 506
                            :content "xaz"
                            :user-id 55}]}}}))
