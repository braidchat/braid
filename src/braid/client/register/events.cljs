(ns braid.client.register.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch reg-fx]]
    [clojure.string :as string]
    [ajax.core :refer [ajax-request]]
    [ajax.edn :refer [edn-request-format edn-response-format]]
    [braid.common.util :refer [slugify]])
  (:import
    [goog Uri]
    [goog.format EmailAddress]))

(defn get-url-param [param]
  (.getParameterValue (.parse Uri js/window.location) (name param)))

(def fields
  [:email :name :url :type])

(def validations
  {:email [(fn [email cb]
             (if (string/blank? email)
               (cb "You need to enter your email.")
               (cb nil)))
           (fn [email cb]
             (if (not (.isValid (EmailAddress. email)))
               (cb "This doesn't look like a valid email.")
               (cb nil)))]
   :name [(fn [name cb]
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

; EVENTS

(reg-event-fx
  :register/initialize
  (fn [{state :db} _]
    {:db (-> state
             (assoc
               :user-auth-section {:user nil
                                   :checking? true
                                   :register? true
                                   :oauth-provider nil}
               :sending? false
               :user-mode :checking
               :action-mode :create-group
               :fields (reduce (fn [memo field]
                                 (assoc memo field
                                   {:value (or (get-url-param field) "")
                                    :typing false
                                    :untouched? true
                                    :validations-left 0
                                    :errors []}))
                               {} fields)))
     :dispatch-n [[:validate-all]
                  [:register.user/remote-check-auth]]}))

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
                   :params {:email (get-in state [:fields :email :value])
                            :slug (get-in state [:fields :url :value])
                            :name (get-in state [:fields :name :value])
                            :type (get-in state [:fields :type :value])}
                   :handler (fn [[_ response]]
                              (dispatch [:handle-registration-response response]))})
    {:db (assoc state :sending? true)}))

(reg-event-fx
  :handle-registration-response
  (fn [{state :db} [_ response]]
    {:db (assoc state :sending? false)}))

; USER AUTH SECTION

(reg-event-fx
  :register.user/set-user
  (fn [{state :db} [_ data]]
    {:db (-> state
             (assoc-in [:user-auth-section :user] data)
             (assoc-in [:user-auth-section :checking?] false)
             (assoc-in [:user-auth-section :oauth-provider] nil))}))

(reg-event-fx
  :register.user/switch-account
  (fn [{state :db} _]
    ; TODO ajax request to log-out
    {:dispatch-n [[:register.user/set-user nil]
                  [:register.user/set-user-register? false]]}))

(reg-event-fx
  :register.user/set-user-register?
  (fn [{state :db} [_ bool]]
    {:db (assoc-in state [:user-auth-section :register?] bool)}))

(reg-event-fx
  :register.user/fake-remote-auth
  (fn [{state :db} _]
    (js/setTimeout (fn []
                     (dispatch
                       [:register.user/set-user
                        {:id "1234"
                         :nickname "rafd"
                         :email "rafal.dittwald@gmail.com"
                         :avatar "https://en.gravatar.com/userimage/612305/740d38e04f1c21f1fb27e76b5f63852a.jpeg"}]))
                   1000)
    {:db (assoc-in state [:user-auth-section :checking?] true)}))

(reg-event-fx
  :register.user/remote-check-auth
  (fn [{state :db} _]
    ; TODO ajax request to check auth status
    (dispatch [:register.user/fake-remote-auth])
    {}))

(reg-event-fx
  :register.user/remote-oauth
  (fn [{state :db} [_ provider]]
    ; TODO kick off oauth process
    (dispatch [:register.user/fake-remote-auth])
    {:db (assoc-in state [:user-auth-section :oauth-provider] provider)}))

(reg-event-fx
  :register.user/remote-log-in
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:register.user/fake-remote-auth])))

(reg-event-fx
  :register.user/remote-register
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:register.user/fake-remote-auth])))

