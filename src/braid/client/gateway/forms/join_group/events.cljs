(ns braid.client.gateway.forms.join-group.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.gateway.helpers :as helpers]))

(reg-event-fx
  :gateway.forms.join-group/initialize
  (fn [{state :db}]
    {:dispatch-n [[:gateway.user-auth/initialize :log-in]
                  [::remote-get-group-info]]}))

(defn get-url-group-id []
  (last (string/split js/window.location #"/")))

(reg-event-fx
  ::handle-group-info
  (fn [{state :db} [_ group-info]]
    {:db (assoc-in state [:join-group :group] (if group-info
                                                       group-info
                                                       {}))}))

(reg-event-fx
  ::handle-error
  (fn [{state :db} [_ group-info]]
    ; TODO
    {}))

(reg-event-fx
  ::submit-form
  (fn [_ _]
    {:dispatch [::remote-join-group]}))

(reg-event-fx
  ::remote-get-group-info
  (fn [_ _]
    {:edn-xhr {:method :get
               :uri (str "/groups/" (get-url-group-id))
               :on-complete
               (fn [response]
                 (dispatch [::handle-group-info response]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [::handle-error k])))}}))

(reg-event-fx
  ::remote-join-group
  (fn [{state :db} _]
    {:edn-xhr {:method :put
               :uri (str "/groups/" (get-url-group-id) "/join")
               :headers {"x-csrf-token" (state :csrf-token)}
               :on-complete
               (fn [response]
                 (set! js/window.location (str "/groups/" (get-url-group-id))))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [::handle-error k])))}}))


