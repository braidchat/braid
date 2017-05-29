(ns braid.client.gateway.forms.create-group.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.common.util :refer [slugify]]
    [braid.client.gateway.helpers :as helpers]
    [braid.client.gateway.forms.create-group.validations :refer [validations]]))

(reg-event-fx
  ::initialize
  (fn [{state :db}]
    {:db (-> state
             (assoc
               :create-group {:sending? false
                              :error nil
                              :validations validations
                              :should-validate? false
                              :fields (helpers/init-fields validations)}))
     :dispatch-n [[:braid.client.gateway.forms.user-auth.events/initialize :register]
                  [::validate-all]]}))

(reg-event-fx
  ::guess-group-url
  (fn [{state :db} _]
    (let [group-name (get-in state [:create-group :fields :group-name :value])
          group-url (get-in state [:create-group :fields :group-url :value])]
      {:db (if (string/blank? group-url)
             (-> state
                 (assoc-in [:create-group :fields :group-url :value] (slugify group-name))
                 (assoc-in [:create-group :fields :group-url :untouched?] false))
             state)
       :dispatch-n [[::clear-errors :group-url]
                    [::validate-field :group-url]]})))

(reg-event-fx
  ::remote-create-group
  (fn [{state :db} _]
    {:db (assoc-in state [:create-group :sending?] true)
     :edn-xhr {:method :put
               :uri "/groups"
               :headers {"x-csrf-token" (state :csrf-token)}
               :params
               {:slug (get-in state [:create-group :fields :group-url :value])
                :name (get-in state [:create-group :fields :group-name :value])
                :type (get-in state [:create-group :fields :group-type :value])}
               :on-complete
               (fn [response]
                 (dispatch [::handle-registration-response response]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [::handle-registration-error k])))}}))

(reg-event-fx
  ::handle-registration-response
  (fn [{state :db} [_ response]]
    (set! js/window.location (str "/groups/" (response :group-id) ))
    {:db (assoc-in state [:create-group :sending?] false)}))

(reg-event-fx
  ::handle-registration-error
  (fn [{state :db} [_ error]]
    {:db (-> state
             (assoc-in [:create-group :sending?] false)
             (assoc-in [:create-group :error] error))}))

(helpers/reg-form-event-fxs :braid.client.gateway.forms.create-group.events :create-group)
