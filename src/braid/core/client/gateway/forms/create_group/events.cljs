(ns braid.core.client.gateway.forms.create-group.events
  (:require
   [braid.core.client.gateway.forms.create-group.validations :refer [validations]]
   [braid.core.client.gateway.helpers :as helpers]
   [braid.core.client.state :refer [reg-event-fx]]
   [braid.core.common.util :refer [slugify]]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch]]
   [braid.core.client.routes :as routes]
   [braid.core.client.schema :as schema]))

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
     :dispatch-n [[:braid.core.client.gateway.forms.user-auth.events/initialize :register]
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
  (fn [{state :db} [_ {:keys [group-id group] :as response}]]
    {:db (-> state
             (assoc-in [:create-group :sending?] false)
             (assoc-in [:groups group-id] group)
             (assoc-in [:temp-threads group-id] (schema/make-temp-thread group-id)))
     :redirect-to (routes/inbox-page-path {:group-id group-id})}))

(reg-event-fx
  ::handle-registration-error
  (fn [{state :db} [_ error]]
    {:db (-> state
             (assoc-in [:create-group :sending?] false)
             (assoc-in [:create-group :error] error))}))

(helpers/reg-form-event-fxs :braid.core.client.gateway.forms.create-group.events :create-group)
