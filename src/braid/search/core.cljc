(ns braid.search.core
  (:require
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    #?@(:clj
         [[braid.chat.db.tag :as tag]
          [braid.chat.db.thread :as thread]
          [braid.search.lucene :as lucene]
          [braid.search.server :as search]]
         :cljs
         [[clojure.string :as string]
          [re-frame.core :refer [dispatch]]
          [spec-tools.data-spec :as ds]
          [braid.core.client.routes :as routes]
          [braid.core.client.state.helpers :as helpers :refer [key-by-id]]
          [braid.search.ui.search-page :refer [search-page-view]]
          [braid.search.ui.search-page-styles :refer [>search-page]]])))

(defn init! []
  #?(:cljs
     (do
       (base/register-state!
         {::state {:query nil
                   :thread-ids nil
                   :loading? false
                   :error? false}}
         {::state {:query (ds/maybe string?)
                   :thread-ids (ds/maybe [any?])
                   :error? boolean?
                   :loading? boolean?}})

       (base/register-events!
         {:braid.search/update-query!
          (fn [{db :db} [_ query]]
            {:dispatch [::set-query! query]
             :dispatch-debounce [:search-redirect
                                 [:go-to
                                  (if (string/blank? query)
                                    (routes/group-page-path {:group-id (db :open-group-id)
                                                             :page-id "inbox"})
                                    (routes/group-page-path {:group-id (db :open-group-id)
                                                             :page-id "search"
                                                             :query-params {:query query}}))]
                                 350]})

          ::set-query!
          (fn [{db :db} [_ query]]
            {:db (assoc-in db [::state :query] query)})

          ::set-results!
          (fn [{db :db} [_ query {:keys [threads thread-ids]}]]
            {:db (-> db
                     (update-in [:threads] #(merge-with merge % (key-by-id threads)))
                     (assoc-in [::state :loading?] false)
                     (update-in [::state] (fn [s]
                                            (if (= (s :query) query)
                                              (assoc s :thread-ids (vec thread-ids))
                                              s))))})

          ::set-error!
          (fn [{db :db} _]
            {:db (-> db
                     (assoc-in [::state :error?] true)
                     (assoc-in [::state :loading?] false))})

          :braid.search/search-history!
          (fn [{db :db} [_ query group-id]]
            (when-not (string/blank? query)
              {:db (-> db
                       (assoc-in [::state :thread-ids] nil)
                       (assoc-in [::state :error?] false)
                       (assoc-in [::state :loading?] true))
               :websocket-send
               (list
                 [::search-ws [query group-id]]
                 15000
                 (fn [reply]
                   (if (:thread-ids reply)
                     (dispatch [::set-results! query reply])
                     (dispatch [::set-error!]))))}))

          ::clear-search!
          (fn [{db :db} _]
            {:db (assoc db ::state {:query nil
                                    :thread-ids nil
                                    :error? false
                                    :loading? false})})})

       (base/register-subs!
         {:braid.search/query
          (fn [state _]
            (get-in state [::state :query]))

          :braid.search/state
          (fn [state _]
            (state ::state))})

       (chat/register-group-page!
         {:key :search
          :view search-page-view
          :on-load (fn [page]
                     (dispatch [::set-query! (page :query)])
                     (dispatch [:braid.search/search-history! (page :query) (page :group-id)]))
          :on-exit (fn [page]
                     (dispatch [::clear-search!]))
          :styles >search-page}))

     :clj
     (do
       (base/register-config-var! :lucene-store-location)

       ;; [TODO] also register for when a message is deleted to remove
       ;; text from index?
       (chat/register-new-message-callback! lucene/index-message!)

       (base/register-server-message-handlers!
         {::search-ws
          (fn [{:as ev-msg :keys [?data ?reply-fn user-id]}]
            ;; this can take a while, so move it to a future
            (future
              (let [user-tags (tag/tag-ids-for-user user-id)
                    filter-tags (fn [t] (update-in t [:tag-ids] (partial into #{} (filter user-tags))))
                    thread-ids (search/search-threads-as user-id ?data)
                    threads (map (comp filter-tags thread/thread-by-id)
                                 (take 25 thread-ids))]
                (when ?reply-fn
                  (?reply-fn {:threads threads :thread-ids thread-ids})))))}))))
