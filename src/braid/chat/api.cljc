(ns braid.chat.api
  (:require
    #?@(:cljs
         [[braid.chat.client.events]
          [braid.chat.client.subs]]
         :clj
         [[braid.chat.db.user]])))

#?(:cljs
   (do
     (defn register-initial-user-data-handler!
       "Add a handler that will run with the initial db & user-info recieved from the server. See `:register-initial-user-data` under `:clj`"
       [f]
       {:pre [(fn? f)]}
       (swap! braid.chat.client.events/initial-user-data-handlers conj f)))

   :clj
   (do
     (defn register-post-create-user-txn!
       "Add a function that will return a sequence of datomic txns to be called when a new user is created. The function will recieve the datomic id of the new user as an argument."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.chat.db.user/post-create-txns conj f))))
