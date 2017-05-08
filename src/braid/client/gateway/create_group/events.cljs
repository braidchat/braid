(ns braid.client.gateway.create-group.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.common.util :refer [slugify]]))

(reg-event-fx
  :gateway.action.create-group/initialize
  (fn [{state :db}]
    {:db (-> state
             (assoc
               :action {:mode :create-group
                        :sending? false
                        :error nil}))}))

(reg-event-fx
  :gateway.action.create-group/guess-group-url
  (fn [{state :db} _]
    (let [group-name (get-in state [:fields :gateway.action.create-group/group-name :value])
          group-url (get-in state [:fields :gateway.action.create-group/group-url :value])]
      {:db (if (string/blank? group-url)
             (-> state
                 (assoc-in [:fields :gateway.action.create-group/group-url :value] (slugify group-name))
                 (assoc-in [:fields :gateway.action.create-group/group-url :untouched?] false))
             state)
       :dispatch-n [[:gateway/clear-errors :gateway.action.create-group/group-url]
                    [:gateway/validate-field :gateway.action.create-group/group-url]]})))

(reg-event-fx
  :gateway.action.create-group/remote-create-group
  (fn [{state :db} _]
    {:db (assoc-in state [:action :sending?] true)
     :edn-xhr {:method :put
               :uri "/groups"
               :params
               {:slug (get-in state [:fields :gateway.action.create-group/group-url :value])
                :name (get-in state [:fields :gateway.action.create-group/group-name :value])
                :type (get-in state [:fields :gateway.action.create-group/group-type :value])}
               :on-complete
               (fn [response]
                 (dispatch [:gateway.action.create-group/handle-registration-response response]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [:gateway.action.create-group/handle-registration-error k])))}}))

(reg-event-fx
  :gateway.action.create-group/handle-registration-response
  (fn [{state :db} [_ response]]
    (set! js/window.location (str "/" (response :group-id) "/inbox"))
    {:db (assoc-in state [:action :sending?] false)}))

(reg-event-fx
  :gateway.action.create-group/handle-registration-error
  (fn [{state :db} [_ error]]
    {:db (-> state
             (assoc-in [:action :sending?] false)
             (assoc-in [:action :error] error))}))