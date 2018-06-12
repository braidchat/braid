(ns braid.core.client.state
  (:require
   [re-frame.core :as api]
   [braid.core.common.util :as util]))

(defn initialize-state
  [db]
  (-> (db ::initial-state)
      (merge (select-keys db [::state-spec
                              ::initial-state]))))

(api/reg-event-fx ::register-state!
  (fn [{db :db} [_ state spec]]
    {:db (-> db
             (update ::initial-state merge state)
             (update ::state-spec merge spec))}))

(defn register-state!
  [state spec]
  ;; Dispatch sync because we want the module setup calls
  ;; to finish before initializing the db
  (api/dispatch-sync [::register-state! state spec]))

(api/reg-sub :braid.state/valid?
  (fn [db _]
    (util/valid? (db ::state-spec) db)))

(defn ^:export validate []
  @(api/subscribe [:braid.state/valid?]))
