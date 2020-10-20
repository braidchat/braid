(ns braid.base.client.state
  (:require
    [clojure.spec.alpha :as s]
    [spec-tools.data-spec :as ds]
    [re-frame.core :as re-frame]
    [braid.core.common.util :as util]))

(defn initialize-state
  [db]
  (-> (db ::initial-state)
      (merge (select-keys db [::state-spec
                              ::initial-state]))))

(re-frame/reg-event-fx ::register-state!
  (fn [{db :db} [_ state spec]]
    {:db (-> db
             (update ::initial-state merge state)
             (update ::state-spec merge spec))}))

(defn register-state!
  [state spec]
  ;; Dispatch sync because we want the module setup calls
  ;; to finish before initializing the db
  (re-frame/dispatch-sync [::register-state! state spec]))

(re-frame/reg-sub :braid.state/valid?
  (fn [db _]
    (util/valid? (db ::state-spec) db)))

(def validate-schema-interceptor
  (re-frame/after
    (fn [db [event-id]]
      (when-let [errors (s/explain-data
                          (ds/spec {:name ::app-state
                                    :spec (db ::state-spec)})
                          db)]
        (js/console.error
          (str
            "Event " event-id
            " caused the state to be invalid:\n")
          (pr-str (map (fn [problem]
                         {:path (problem :path)
                          :pred (problem :pred)})
                       (::s/problems errors))))))))

(if ^boolean goog.DEBUG
  (defn reg-event-fx
    ([id handler-fn]
     (reg-event-fx id nil handler-fn))
    ([id interceptors handler-fn]
     (re-frame/reg-event-fx
       id
       [validate-schema-interceptor
        interceptors]
       handler-fn)))
  (def reg-event-fx re-frame/reg-event-fx))
