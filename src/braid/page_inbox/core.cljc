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

