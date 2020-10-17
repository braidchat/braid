(ns braid.stars.core
  "Allows users to star threads (for themselves) and view starred threads seperately."
  (:require
    [braid.core.api :as core]
    [braid.chat.api :as chat]
    [braid.base.api :as base]
    #?@(:cljs
         [[re-frame.core :refer [subscribe dispatch]]
          [braid.core.client.routes :as routes]
          [braid.core.client.ui.views.threads :refer [threads-view]]]
         :clj
         [[datomic.api :as d]
          [braid.core.server.db :as db]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-group-page!
         {:key :starred
          :on-load (fn [_]
                     (dispatch [:braid.stars/load-starred-threads!]))
          :view (fn []
                  (let [threads @(subscribe [:braid.stars/starred-threads])
                        page @(subscribe [:page])]
                    [:div.page.recent
                     (if (and (not (page :loading?)) (empty? threads))
                       [:div.content
                        [:p "No starred threads"]]
                       [threads-view {:threads threads}])]))})

       (core/register-group-header-button!
         {:title "Starred Threads"
          :class "starred"
          :icon \uf005
          :priority 9
          :route-fn routes/group-page-path
          :route-args {:page-id "starred"}})

       (core/register-thread-header-item!
         {:priority 100
          :view
          (fn [thread]
            (let [starred? @(subscribe [:braid.stars/thread-starred? (thread :id)])]
              (when-not (thread :new?)
                [:div.star
                 {:class (if starred?
                           "starred"
                           "not-starred")
                  :title (if starred?
                           "Star this thread"
                           "Remove star from this thread")
                  :on-click (fn [e]
                              (if starred?
                                (dispatch [:braid.stars/unstar-thread! (thread :id)])
                                (dispatch [:braid.stars/star-thread! (thread :id)])))}
                 \uf005])))})

       (base/register-styles!
         [:.head
          [:>.star
           {:display "inline-block"
            :font-size "1.25em"
            :margin-right "0.25rem"
            :cursor "pointer"
            :transform "rotate(-90deg)"
            :font-family "fontawesome"}

           [:&.starred
            {:color "#f3c657"
             :-webkit-text-stroke "#a5832d 0.5px"
             :text-shadow "-1px 0 1px #a5832d"}]

           [:&.not-starred
            {:color "#ddd"
             :-webkit-text-stroke "#999 0.5px"
             :text-shadow "-1px 0 1px #999"}]]])

       (core/register-state!
         {:braid.stars/starred-thread-ids {}}
         {:braid.stars/starred-thread-ids {uuid? #{uuid?}}})

       (base/register-events!
         {:braid.stars/load-starred-threads!
          (fn [{db :db} _]
            {:dispatch [:load-threads {:thread-ids (get-in db [:braid.stars/starred-thread-ids (db :open-group-id)])}]})

          :braid.stars/star-thread!
          (fn [{db :db} [_ thread-id]]
            (let [group-id (db :open-group-id)]
              ; check, to prevent loop
              (when-not (contains? (get-in db [:braid.stars/starred-thread-ids group-id]) thread-id)
                {:db (update-in db [:braid.stars/starred-thread-ids group-id] (fnil conj #{}) thread-id)
                 :websocket-send [[:braid.stars.ws/star-thread! thread-id]]})))

          :braid.stars/unstar-thread!
          (fn [{db :db} [_ thread-id]]
            (let [group-id (db :open-group-id)]
              ; check, to prevent loop
              (when (contains? (get-in db [:braid.stars/starred-thread-ids group-id]) thread-id)
                {:db (update-in db [:braid.stars/starred-thread-ids group-id] disj thread-id)
                 :websocket-send [[:braid.stars.ws/unstar-thread! thread-id]]})))})

       (base/register-subs!
         {:braid.stars/starred-threads
          (fn [db _]
            (let [thread-ids (get-in db [:braid.stars/starred-thread-ids (db :open-group-id)])]
              (vals (select-keys (db :threads) thread-ids))))

          :braid.stars/thread-starred?
          (fn [db [_ thread-id]]
            (contains? (get-in db [:braid.stars/starred-thread-ids (db :open-group-id)]) thread-id))})

       (core/register-incoming-socket-message-handlers!
         {:braid.stars.ws/star-thread!
          (fn [_ thread-id]
            (dispatch [:braid.stars/star-thread! thread-id]))

          :braid.stars.ws/unstar-thread!
          (fn [_ thread-id]
            (dispatch [:braid.stars/unstar-thread! thread-id]))})

       (chat/register-initial-user-data-handler!
         (fn
           [db data]
           (assoc db :braid.stars/starred-thread-ids
             (data :braid.stars/starred-thread-ids)))))

     :clj
     (do
       (core/register-db-schema!
         [{:db/ident :user/starred-thread
           :db/valueType :db.type/ref
           :db/cardinality :db.cardinality/many}])

       (core/register-initial-user-data!
         (fn [user-id]
           {:braid.stars/starred-thread-ids
            (->> (d/q '[:find ?thread-id ?group-id
                        :in $ ?user-id
                        :where
                        [?u :user/id ?user-id]
                        [?u :user/starred-thread ?t]
                        [?t :thread/id ?thread-id]
                        [?t :thread/group ?g]
                        [?g :group/id ?group-id]]
                      (db/db)
                      user-id)
                 (reduce (fn [memo [thread-id group-id ]]
                           (update memo group-id (fnil conj #{}) thread-id))
                         {}))}))

       (core/register-server-message-handlers!
         {:braid.stars.ws/star-thread!
          (fn [{user-id :user-id thread-id :?data}]
            {:db-run-txns! [[:db/add [:user/id user-id] :user/starred-thread [:thread/id thread-id]]]
             :chsk-send! [user-id [:braid.stars.ws/star-thread! thread-id]]})

          :braid.stars.ws/unstar-thread!
          (fn [{user-id :user-id thread-id :?data}]
            {:db-run-txns! [[:db/retract [:user/id user-id] :user/starred-thread [:thread/id thread-id]]]
             :chsk-send! [user-id [:braid.stars.ws/unstar-thread! thread-id]]})}))))
