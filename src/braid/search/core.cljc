(ns braid.search.core
  (:require
    [braid.core.api :as core]
    #?@(:clj
         [[braid.search.server :as search]
          [braid.core.server.db.tag :as tag]
          [braid.core.server.db.thread :as thread]]
         :cljs
         [[re-frame.core :refer [dispatch]]
          [braid.search.ui.search-page-styles :refer [>search-page]]
          [braid.search.ui.search-page :refer [search-page-view]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-group-page!
         {:key :search
          :view search-page-view
          :on-load (fn [page]
                     (dispatch [:set-page-loading true])
                     (dispatch [:search-history [(page :query) (page :group-id)]]))
          :styles >search-page}))

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
