(ns braid.search.core
  (:require
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    [braid.search.api :as search-api]
    #?@(:clj
         [[braid.chat.db.group :as group]
          [braid.chat.db.thread :as thread]
          [braid.search.lucene :as lucene]
          [braid.search.server :as search]
          [braid.search.threads :as threads-search]
          [braid.search.tags :as tags-search]
          [braid.search.users :as users-search]]
         :cljs
         [[clojure.string :as string]
          [re-frame.core :refer [dispatch]]
          [spec-tools.data-spec :as ds]
          [braid.core.client.routes :as routes]
          [braid.search.ui.search-page :refer [search-page-view]]
          [braid.search.ui.thread-results :as thread-results]
          [braid.search.ui.user-results :as user-results]
          [braid.search.ui.tag-results :as tag-results]
          [braid.search.ui.search-page-styles :refer [>search-page]]])))

(defn init! []
  #?(:cljs
     (do
       (base/register-state!
         {::state {:query nil
                   :results nil
                   :loading? false
                   :error? false}}
         {::state {:query (ds/maybe string?)
                   :results (ds/maybe {keyword? any?})
                   :error? boolean?
                   :loading? boolean?}})

       (base/register-events!
         {:braid.search/update-query!
          (fn [{db :db} [_ query]]
            {:dispatch [::set-query! query]
             :dispatch-debounce [:search-redirect
                                 [:go-to!
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
          (fn [{db :db} [_ query results]]
            {:db (-> db
                     (assoc-in [::state :loading?] false)
                     (update-in [::state] (fn [s]
                                            (if (= (s :query) query)
                                              (assoc s :results results)
                                              s))))})

          ::set-error!!
          (fn [{db :db} _]
            {:db (-> db
                     (assoc-in [::state :error?] true)
                     (assoc-in [::state :loading?] false))})

          :braid.search/search-history!
          (fn [{db :db} [_ query group-id]]
            (when-not (string/blank? query)
              {:db (-> db
                       (assoc-in [::state :results] nil)
                       (assoc-in [::state :error?] false)
                       (assoc-in [::state :loading?] true))
               :websocket-send
               (list
                 [::search-ws [query group-id]]
                 15000
                 (fn [{:keys [results]}]
                   (if results
                     (dispatch [::set-results! query results])
                     (dispatch [::set-error!!]))))}))

          ::clear-search!
          (fn [{db :db} _]
            {:db (assoc db ::state {:query nil
                                    :results nil
                                    :error? false
                                    :loading? false})})})

       (base/register-subs!
         {:braid.search/query
          (fn [state _]
            (get-in state [::state :query]))

          :braid.search/state
          (fn [state _]
            (state ::state))})

       (search-api/register-search-results-view!
         :user
         {:view user-results/search-users-view
          :styles user-results/styles
          :priority 1})
       (search-api/register-search-results-view!
         :tag
         {:view tag-results/search-tags-view
          :styles tag-results/styles
          :priority 10})
       (search-api/register-search-results-view!
         :thread
         {:view thread-results/search-threads-view
          :styles thread-results/styles
          :priority 100})

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

       (search-api/register-search-function! threads-search/search-threads-by-tag)
       (search-api/register-search-function! threads-search/search-threads-by-user)
       (search-api/register-search-function! threads-search/search-threads-by-full-text)
       (search-api/register-search-auth-check!
         :thread
         (fn [user-id search-results]
           (filter (fn [{:keys [thread-id]}]
                     (thread/user-can-see-thread? user-id thread-id))
                   search-results)))
       (search-api/register-search-function! users-search/search-users-by-name)
       (search-api/register-search-function! tags-search/search-tags-by-name)

       ;; [TODO] also register for when a message is deleted to remove
       ;; text from index?
       (chat/register-new-message-callback! lucene/index-message!)

       (base/register-server-message-handlers!
         {::search-ws
          (fn [{:keys [?reply-fn user-id] [query group-id] :?data}]
            ;; this can take a while, so move it to a future
            (do #_future
                (when (and group-id (group/user-in-group? user-id group-id))
                  (let [search-results (search/search-as {:user-id user-id
                                                          :group-id group-id
                                                          :query query})]
                    (when ?reply-fn
                      (?reply-fn {:results search-results}))))))}))))
