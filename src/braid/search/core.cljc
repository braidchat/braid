(ns braid.search.core
  (:require
    [braid.core.api :as core]
    #?@(:clj
         [[braid.search.server :as search]
          [braid.core.server.db.tag :as tag]
          [braid.core.server.db.thread :as thread]]
         :cljs
         [])))

(defn init! []
  #?(:cljs
     (do)
     :clj
     (do
       (core/register-server-message-handlers!
         {:braid.server/search
          (fn [{:as ev-msg :keys [?data ?reply-fn user-id]}]
            ; this can take a while, so move it to a future
            (future
              (let [user-tags (tag/tag-ids-for-user user-id)
                    filter-tags (fn [t] (update-in t [:tag-ids] (partial into #{} (filter user-tags))))
                    thread-ids (search/search-threads-as user-id ?data)
                    threads (map (comp filter-tags thread/thread-by-id)
                                 (take 25 thread-ids))]
                (when ?reply-fn
                  (?reply-fn {:threads threads :thread-ids thread-ids})))))}))))
