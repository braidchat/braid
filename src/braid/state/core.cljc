(ns braid.state.core
  (:require
    [schema.core :as s]
    [braid.core.api :as api]
    #?(:clj [mount.core :refer [defstate]])))

(defn initialize-state
  [db]
  (-> (db ::initial-state)
      (merge (select-keys db [::state-spec
                              ::initial-state]))))

(api/reg-event-fx ::register-state!
  (fn [{db :db} [_ state spec]]
    {:db (-> db
             (merge state)
             (update ::initial-state merge state)
             (update ::state-spec merge spec))}))

(defn ^:api register-state!
  [state spec]
  (api/dispatch [::register-state! state spec]))

(api/reg-sub :braid.state/valid?
  (fn [db _]
    (let [validator (s/validator (db ::state-spec))]
      (validator db))))

(defn init! []
  (register-state!
    {::initial-state {}
     ::state-spec {}}
    {::initial-state s/Any
     ::state-spec {s/Keyword s/Any}}))

(defn ^:export validate []
  @(api/subscribe [:braid.state/valid?]))

#?(:clj
    (defstate state
      :start (init!)))
