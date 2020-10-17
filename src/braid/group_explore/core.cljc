(ns braid.group-explore.core
  (:require
    [braid.core.api :as core]
    #?@(:clj
         [[datomic.api :as d]
          [braid.core.server.db :as db]
          [braid.chat.db.group :as group]
          [braid.chat.db.common :refer [db->group group-pull-pattern]]]
        :cljs
         [[re-frame.core :refer [dispatch]]
          [braid.group-explore.views :as views]
          [braid.group-explore.styles :as styles]
          [braid.core.common.schema :as schema ]])))

#?(:clj
   (defn- get-public-groups
     []
     (->> (d/q '[:find (pull ?g pull-pattern) ?created (max ?updated)
                 :in $ pull-pattern
                 :where
                 [?g :group/id _ ?created-tx]
                 [?created-tx :db/txInstant ?created]
                 ;; group "updated" when a message is posted to it
                 ;; note that this means groups with no messages won't
                 ;; show up...but that's probably reasonable
                 [?t :thread/group ?g]
                 [?m :message/thread ?t ?updated-tx]
                 [?updated-tx :db/txInstant ?updated]]
               (db/db) group-pull-pattern)
          (into #{}
                (comp (map (fn [[g c u]] (assoc (db->group g)
                                           :created-at c :updated-at u)))
                      (filter :public?)
                      (map #(select-keys % [:id :name :slug :avatar :intro
                                            :users-count :created-at :updated-at]))))))

   :cljs
   (do
     (def PublicGroup
       (select-keys schema/Group
                    [:id :name :slug :avatar :intro
                     :users-count :created-at :updated-at]))))

(defn init! []
  #?(:clj
     (do
       (core/register-public-http-route!
         [:get "/groups"
          (fn [_]
            {:status 200
             :body (get-public-groups)})]))

     :cljs
     (do
       (core/register-state!
         {::public-groups #{}}
         {::public-groups #{PublicGroup}})

       (core/register-events!
         {::-store-public-groups
          (fn [{db :db} [_ groups]]
            {:db (assoc db ::public-groups groups)})

          ::load-public-groups
          (fn [_ _]
            {:edn-xhr
             {:uri "/groups"
              :method :get
              :on-complete
              (fn [resp]
                (dispatch [::-store-public-groups resp]))
              :on-error
              (fn [_]
                (dispatch [:braid.notices/display!
                           [:load-public-groups
                            "Failed to load public groups list"
                            :error]]))}})})

       (core/register-subs!
         {:braid.group-explore/public-groups
          (fn [state _]
            (state ::public-groups))

          :braid.group-explore/subscribed-group-ids
          (fn [db _]
            (into #{}
                  (comp (remove (fn [[id group]] (:readonly group)))
                        (map first))
                  (:groups db)))})

       (core/register-system-page!
         {:key :group-explore
          :on-load (fn [_]
                     (dispatch [::load-public-groups]))
          :view views/group-explore-page-view
          :styles styles/>group-explore-page}))))
