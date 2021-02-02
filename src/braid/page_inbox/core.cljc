(ns braid.page-inbox.core
  (:require
    [braid.lib.uuid :as uuid]
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    #?@(:cljs
         [[braid.page-inbox.ui :as ui]
          [braid.core.client.state.helpers :as helpers]])))

(defn init! []
  #?(:cljs
     (do
       (chat/register-group-page!
         {:key :inbox
          :on-load (fn [_]
                     )
          :view ui/inbox-page-view})

       (base/register-subs!
         {:open-threads
          ;; TODO could be made more efficient by depending on other subs
          ;; or using reg-sub-raw and reactions
          (fn [state [_ group-id]]
            (let [user-id (get-in state [:session :user-id])
                  open-thread-ids (get-in state [:user :open-thread-ids])
                  threads (state :threads)
                  open-threads (vals (select-keys threads open-thread-ids))]
              (->> open-threads
                   (filter (fn [thread]
                             (= (thread :group-id) group-id)))
                   ;; sort by last message sent by logged-in user, most recent first
                   (sort-by
                     (fn [thread]
                       (->> (thread :messages)
                            (filter (fn [m] (= (m :user-id) user-id)))
                            (map :created-at)
                            (apply max))))
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
               :dispatch [:focus-thread (thread-opts :id)]}))})

       (base/register-styles!
         [:.app>.main ui/styles]))))

