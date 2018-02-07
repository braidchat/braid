(ns braid.core.client.state
  (:require
   [re-frame.core :as api]
   [schema.core :as s]))

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
  ([f]
   (let [[state spec] (f)]
     (register-state! state spec)))
  ([state spec]
   ;; Dispatch sync because we want the module setup calls
   ;; to finish before initializing the db
   (api/dispatch-sync [::register-state! state spec])))

(api/reg-sub :braid.state/valid?
  (fn [db _]
    (let [validator (s/validator (db ::state-spec))]
      (validator db))))

(defn ^:export validate []
  @(api/subscribe [:braid.state/valid?]))
