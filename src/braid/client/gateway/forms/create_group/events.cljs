(ns braid.client.gateway.forms.create-group.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.common.util :refer [slugify]]
    [braid.client.gateway.helpers :as helpers]
    [braid.client.gateway.forms.create-group.validations :refer [validations]]))

(reg-event-fx
  :gateway.action.create-group/initialize
  (fn [{state :db}]
    {:db (-> state
             (assoc
               :create-group {:sending? false
                              :error nil
                              :validations validations
                              :should-validate? false
                              :fields (helpers/init-fields validations)}))
     :dispatch-n [[:gateway.user-auth/initialize :register]
                  [:gateway.create-group/validate-all]]}))

(reg-event-fx
  :gateway.action.create-group/guess-group-url
  (fn [{state :db} _]
    (let [group-name (get-in state [:create-group :fields :gateway.action.create-group/group-name :value])
          group-url (get-in state [:create-group :fields :gateway.action.create-group/group-url :value])]
      {:db (if (string/blank? group-url)
             (-> state
                 (assoc-in [:fields :gateway.action.create-group/group-url :value] (slugify group-name))
                 (assoc-in [:fields :gateway.action.create-group/group-url :untouched?] false))
             state)
       :dispatch-n [[:gateway.create-group/clear-errors :gateway.action.create-group/group-url]
                    [:gateway.create-group/validate-field :gateway.action.create-group/group-url]]})))

(reg-event-fx
  :gateway.action.create-group/remote-create-group
  (fn [{state :db} _]
    {:db (assoc-in state [:create-group :sending?] true)
     :edn-xhr {:method :put
               :uri "/groups"
               :headers {"x-csrf-token" (state :csrf-token)}
               :params
               {:slug (get-in state [:create-group :fields :gateway.action.create-group/group-url :value])
                :name (get-in state [:create-group :fields :gateway.action.create-group/group-name :value])
                :type (get-in state [:create-group :fields :gateway.action.create-group/group-type :value])}
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
    (set! js/window.location (str "/groups/" (response :group-id) ))
    {:db (assoc-in state [:create-group :sending?] false)}))

(reg-event-fx
  :gateway.action.create-group/handle-registration-error
  (fn [{state :db} [_ error]]
    {:db (-> state
             (assoc-in [:create-group :sending?] false)
             (assoc-in [:create-group :error] error))}))

(helpers/reg-form-event-fxs :gateway.create-group :create-group)
