(ns braid.client.register.create-group.events
  (:require
    [clojure.string :as string]
    [ajax.core :refer [ajax-request]]
    [ajax.edn :refer [edn-request-format edn-response-format]]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.common.util :refer [slugify]]))

(reg-event-fx
  :guess-group-url
  (fn [{state :db} _]
    (let [group-name (get-in state [:fields :name :value])
          group-url (get-in state [:fields :url :value])]
      {:db (if (string/blank? group-url)
             (-> state
                 (assoc-in [:fields :url :value] (slugify group-name))
                 (assoc-in [:fields :url :untouched?] false))
             state)
       :dispatch-n [[:clear-errors :url]
                    [:validate-field :url]]})))

(reg-event-fx
  :submit-form
  (fn [{state :db} _]
    (if-let [all-valid? (every? true? (map (fn [[_ v]] (empty? (v :errors))) (state :fields)))]
      {:dispatch [:send-registration-request]}
      {:dispatch [:touch-all-fields]})))

(reg-event-fx
  :send-registration-request
  (fn [{state :db} _]
    (ajax-request {:uri (str "//" js/window.api_domain "/registration/register")
                   :method :put
                   :format (edn-request-format)
                   :response-format (edn-response-format)
                   :params {:email (get-in state [:fields :email :value])
                            :slug (get-in state [:fields :url :value])
                            :name (get-in state [:fields :name :value])
                            :type (get-in state [:fields :type :value])}
                   :handler (fn [[_ response]]
                              (dispatch [:handle-registration-response response]))})
    {:db (assoc-in state [:action :sending?] true)}))

(reg-event-fx
  :handle-registration-response
  (fn [{state :db} [_ response]]
    {:db (assoc-in state [:action :sending?] false)}))
