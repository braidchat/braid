(ns braid.state.core
  (:require
    [schema.core :as s]
    [braid.core.api :as api]))

(defn initialize-state
  [db]
  (-> (db ::initial-state)
      (merge (select-keys db [::state-spec
                              ::initial-state]))))

(api/reg-event-fx :braid.state/register-state!
  (fn [{db :db} [_ state spec]]
    {:db (-> db
             (merge state)
             (update ::initial-state merge state)
             (update ::state-spec merge spec))}))

(api/reg-event-fx :braid.state/initialize-state!
  (fn [{db :db} _]
    {:db (initialize-state db)}))

(api/reg-sub :braid.state/valid?
  (fn [db _]
    (let [validator (s/validator (db ::state-spec))]
      (validator db))))

(defn init! []
  (api/dispatch [:braid.state/register-state!
                 {::initial-state {}
                  ::state-spec {}}
                 {::initial-state s/Any
                  ::state-spec {s/Keyword s/Any}}]))

(defn ^:export validate []
  @(api/subscribe [:braid.state/valid?]))


