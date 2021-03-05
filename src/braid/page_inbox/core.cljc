(ns braid.page-inbox.core
  (:require
    [braid.lib.uuid :as uuid]
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    #?@(:clj
         [[braid.page-inbox.commands :as commands]]
        :cljs
         [[braid.page-inbox.ui :as ui]
          [braid.page-inbox.styles :as styles]
          [braid.core.client.state.helpers :as helpers]])))



(defn init! []
  #?(:clj
     (do
       (base/register-commands!
         commands/commands))
     :cljs
     (do
       (base/register-initial-user-data-handler!
         (fn [db data]
           (assoc-in db [:user :open-thread-ids]
             (set (map :id (data :user-threads))))))

       (chat/register-group-page!
         {:key :inbox
          :on-load (fn [_]
                     )
          :view ui/inbox-page-view})

       (base/register-subs!
         {:open-thread-ids
          (fn [state _]
            (get-in state [:user :open-thread-ids]))

          :open-threads
          ;; TODO could be made more efficient by depending on other subs
          ;; or using reg-sub-raw and reactions
          (fn [state [_ group-id]]
            (let [open-thread-ids (get-in state [:user :open-thread-ids])
                  threads (state :threads)
                  open-threads (vals (select-keys threads open-thread-ids))
                  now (js/Date.)]
              (->> open-threads
                   (filter (fn [thread]
                             (= (thread :group-id) group-id)))
                   (sort-by
                     (fn [thread]
                       (or (->> (thread :messages)
                                (map :created-at)
                                (apply max))
                           ;; treat threads with no messages as being just created
                           now)))
                   reverse)))})

       (base/register-events!
         {:create-thread!
          (fn [{db :db} [_ thread-opts]]
            (let [thread-opts (merge
                                thread-opts
                                {:id (uuid/squuid)})]
              {:db (-> db
                       (helpers/create-thread thread-opts)
                       (helpers/add-to-open-threads (:id thread-opts)))
               :command [:braid.chat/create-thread!
                         {:thread-id (:id thread-opts)
                          :group-id (:group-id thread-opts)}]
               :dispatch [:focus-thread! (thread-opts :id)]}))

          :clear-inbox!
          (fn [{db :db} [_ _]]
            {:dispatch-n
             (into ()
                   (comp
                     (filter (fn [thread] (= (db :open-group-id) (thread :group-id))))
                     (map :id)
                     (map (fn [id] [:hide-thread! {:thread-id id :local-only? false}])))
                   (-> (db :threads)
                       (select-keys (get-in db [:user :open-thread-ids]))
                       vals))})

          :hide-thread!
          (fn [{db :db} [_ {:keys [thread-id local-only?]}]]
            {:db (update-in db [:user :open-thread-ids] disj thread-id)
             :command (when-not local-only?
                        [:braid.inbox/hide-thread!
                         {:thread-id thread-id}])})

          :reopen-thread!
          (fn [{db :db} [_ thread-id]]
            {:db (update-in db [:user :open-thread-ids] conj thread-id)
             :command [:braid.inbox/show-thread!
                       {:thread-id thread-id}]})})

       (base/register-styles!
         [:.app>.main styles/styles]))))

