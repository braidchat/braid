(ns braid.mobile.remote)

(def fake-response
  {:user {}
   :groups {1 {:id 1
               :name "Foo"}
            2 {:id 2
               :name "Bar"}
            3 {:id 3
               :name "Baz"}}
   :users {44 {:id 44
               :name "therealbatman"}
           55 {:id 55
               :name "brucewayne"}}
   :tags {444 {:id 444
               :name "watercooler"
               :group-id 1}
          555 {:id 555
               :name "braid"
               :group-id 1}}
   :active-group-id 1
   :threads {4 {:id 4
                :group-id 1
                :tag-ids [444 555]
                :messages [{:id 400
                            :content "Alright! it's time to test some messages out."
                            :user-id 44 }
                           {:id 401
                            :content "Reply to this please."
                            :user-id 44}
                           {:id 402
                            :content "OK"
                            :user-id 55}]}
             5 {:id 5
                :group-id 1
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
                :group-id 2
                :messages [{:id 504
                            :content "xoo"
                            :user-id 44}
                           {:id 505
                            :content "xar"
                            :user-id 55}
                           {:id 506
                            :content "xaz"
                            :user-id 55}]}}})
