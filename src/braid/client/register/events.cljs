(ns braid.client.register.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch reg-fx]]
    [clojure.string :as string]))

(defn ajax-request [config]
  (js/setTimeout (fn []
                   ((config :handler) true))
                 1000))

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
             {:path ""
              :handler (fn [valid?]
                         (if valid?
                           (cb nil)
                           (cb "Your group URL is already taken; try another.")))}))]
   :type [(fn [type cb]
            (when (string/blank? type)
              (cb "You need to select a group type")
              (cb nil)))]})

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
    {:db (assoc state :fields
           (reduce (fn [memo field]
                     (assoc memo field
                       {:value ""
                        :focused? nil
                        :validations-left 0
                        :errors []}))
                   {} fields))
     :dispatch [:validate-all]}))

(reg-event-db
  :focus
  (fn [state [_ field]]
    (assoc-in state [:fields field :focused?] true)))

(reg-event-db
  :blur
  (fn [state [_ field]]
    (assoc-in state [:fields field :focused?] false)))

(reg-event-fx
  :update-value
  (fn [{state :db} [_ field value]]
    {:db (assoc-in state [:fields field :value] value)
     :dispatch [:validate-field field]}))

(reg-event-db
  :update-field-status
  (fn [state [_ field error]]
    (if error
      (-> state
          (update-in [:fields field :errors] conj error)
          (update-in [:fields field :validations-left] dec))
      (-> state
          (update-in [:fields field :validations-left] dec)))))

(reg-event-fx
  :validate-field
  (fn [{state :db} [_ field]]
    (let [validator-fns (validations field)
          field-value (get-in state [:fields field :value])]
      {:db (-> state
               (assoc-in [:fields field :errors] [])
               (assoc-in [:fields field :validations-left] (count validator-fns)))
       :validate-n [[field field-value]]})))

(reg-event-fx
  :validate-all
  (fn [{state :db} _]
    {:validate-n (for [field fields]
                   [field (get-in state [:fields field :value])])}))

