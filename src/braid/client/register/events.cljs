(ns braid.client.register.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch reg-fx]]
    [clojure.string :as string]
    [ajax.core :refer [ajax-request ]]
    [ajax.edn :refer [edn-request-format edn-response-format]]))

(def fields
  [:name :url :type])

(def validations
  {:name [(fn [name cb]
            (if (string/blank? name)
              (cb "Your group needs a name.")
              (cb nil)))]
   :url [(fn [url cb]
           (if (string/blank? url)
             (cb "Your group needs a URL.")
             (cb nil)))
         (fn [url cb]
           (if (not (re-matches #"[a-z0-9-]*" url))
             (cb "Your URL can only contain lowercase letters, numbers or dashes.")
             (cb nil)))
         (fn [url cb]
           (if (re-matches #"-.*" url)
             (cb "Your URL can't start with a dash.")
             (cb nil)))
         (fn [url cb]
           (if (re-matches #".*-" url)
             (cb "Your URL can't end with a dash.")
             (cb nil)))
         (fn [url cb]
           (ajax-request
             {:uri (str "//" js/window.api_domain "/registration/check-slug-unique")
              :method :get
              :format (edn-request-format)
              :response-format (edn-response-format)
              :params {:slug url}
              :handler (fn [[_ valid?]]
                         (if valid?
                           (cb nil)
                           (cb "Your group URL is already taken; try another.")))}))]
   :type [(fn [type cb]
            (when (string/blank? type)
              (cb "You need to select a group type.")
              (cb nil)))]})

(defonce timeouts
  (atom {}))

(reg-fx :dispatch-debounce
        (fn [[id event-vec n]]
          (js/clearTimeout (@timeouts id))
          (swap! timeouts assoc id
                 (js/setTimeout (fn []
                                  (dispatch event-vec)
                                  (swap! timeouts dissoc id))
                                n))))

(reg-fx :stop-debounce
        (fn [id]
          (js/clearTimeout (@timeouts id))
          (swap! timeouts dissoc id)))

(reg-fx :validate-n
  (fn [to-validate]
    (doseq [[field field-value] to-validate]
      (doseq [validator-fn (validations field)]
        (validator-fn
          field-value
          (fn [error]
            (dispatch [:update-field-status field error])))))))

(reg-event-fx
  :register/initialize
  (fn [{state :db} _]
    {:db (assoc state
           :sending? false
           :fields (reduce (fn [memo field]
                             (assoc memo field
                               {:value ""
                                :typing false
                                :untouched? true
                                :validations-left 0
                                :errors []}))
                           {} fields))
     :dispatch [:validate-all]}))

(reg-event-fx
  :blur
  (fn [{state :db} [_ field]]
    {:db (assoc-in state [:fields field :untouched?] false)
     ; use dispatch-debounce
     ; to cancel possible other identical debounced dispatch
     :dispatch-debounce [field [:stop-typing field] 0]}))

(reg-event-db
  :clear-errors
  (fn [state [_ field]]
    (assoc-in state [:fields field :errors] [])))

(reg-event-fx
  :stop-typing
  (fn [{state :db} [_ field]]
    (when (get-in state [:fields field :typing?])
      {:db (assoc-in state [:fields field :typing?] false)
       :dispatch [:validate-field field]})))


(defn slugify [text]
  (when text
    (-> text
        string/trim
        string/lower-case
        (string/replace #"[ -+|,/?%#&\.\!:$'@]*" ""))))

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
  :update-value
  (fn [{state :db} [_ field value]]
    {:db (-> state
             (assoc-in [:fields field :value] value)
             (assoc-in [:fields field :typing?] true)
             (assoc-in [:fields field :untouched?] false))
     :dispatch [:clear-errors field]
     :dispatch-debounce [field [:stop-typing field] 500]}))

(reg-event-db
  :update-field-status
  (fn [state [_ field error]]
    (if error
      (-> state
          (update-in [:fields field :errors] conj error)
          (update-in [:fields field :validations-left] dec))
      (-> state
          (update-in [:fields field :validations-left] dec)))))

(reg-event-db
  :touch-all-fields
  (fn [state _]
    (update state :fields (fn [fields]
                            (reduce (fn [memo [k v]]
                                      (assoc memo k (assoc v :untouched? false))) {}  fields)))))

(reg-event-fx
  :validate-field
  (fn [{state :db} [_ field]]
    (let [validator-fns (validations field)
          field-value (get-in state [:fields field :value])]
      {:db (assoc-in state [:fields field :validations-left] (count validator-fns))
       :validate-n [[field field-value]]})))

(reg-event-fx
  :validate-all
  (fn [{state :db} _]
    {:validate-n (for [field fields]
                   [field (get-in state [:fields field :value])])}))

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
                   :params {:slug (get-in state [:fields :url :value])
                            :name (get-in state [:fields :name :value])
                            :type (get-in state [:fields :type :value])}
                   :handler (fn [[_ response]]
                              (dispatch [:handle-registration-response response]))})
    {:db (assoc state :sending? true)}))

(reg-event-fx
  :handle-registration-response
  (fn [{state :db} [_ response]]
    {:db (assoc state :sending? false)}))
