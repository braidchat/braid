(ns braid.search.core
  (:require
    [braid.core.api :as core]
    #?@(:clj
         [[braid.search.server :as search]
          [braid.core.server.db.tag :as tag]
          [braid.core.server.db.thread :as thread]]
         :cljs
         [[re-frame.core :refer [dispatch]]
          [braid.core.client.state.helpers :as helpers :refer [key-by-id]]
          [braid.search.ui.search-page-styles :refer [>search-page]]
          [braid.search.ui.search-page :refer [search-page-view]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-events!
         {:set-search-query
          (fn [{db :db} [_ query]]
            {:db (assoc-in db [:page :query] query)})

          :set-search-results
          (fn [{db :db} [_ [query {:keys [threads thread-ids] :as reply}]]]
            {:db (-> db
                     (update-in [:threads] #(merge-with merge % (key-by-id threads)))
                     (update-in [:page] (fn [p] (if (= (p :query) query)
                                                  (assoc p :thread-ids thread-ids)
                                                  p))))})

          :search-history
          (fn [{state :db} [_ [query group-id]]]
            (when query
              {:websocket-send
               (list
                 [:braid.server/search [query group-id]]
                 15000
                 (fn [reply]
                   (dispatch [:set-page-loading false])
                   (if (:thread-ids reply)
                     (dispatch [:set-search-results [query reply]])
                     (dispatch [:set-page-error true]))))
               :dispatch [:set-page-error false]}))})

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
