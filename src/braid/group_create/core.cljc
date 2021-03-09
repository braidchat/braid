(ns braid.group-create.core
  (:require
   [clojure.string :as string]
   [braid.base.api :as base]
   [braid.core.common.util :refer [slugify]]
   [braid.group-explore.api :as group-explore.api]
   #?@(:clj
       [[braid.chat.db.group :as group]
        [braid.chat.predicates :as p]
        [braid.core.server.db :as db]
        [braid.core.server.routes.helpers :refer [edn-response]]]
       :cljs
       [[re-frame.core :refer [dispatch]]
        [braid.core.client.gateway.helpers :as helpers]
        [braid.core.client.routes :as routes]
        [braid.group-create.styles :as styles]
        [braid.group-create.validations :refer [validations]]
        [braid.group-create.views :as views]])))

(defn init! []
  #?(:clj
     (do
       (base/register-public-http-route!
        [:get "/registration/check-slug-unique"
         (fn [req]
           (edn-response (not (p/group-with-slug-exists? (db/db) (get-in req [:params :slug])))))]))
     :cljs
     (do
       (base/register-events!
        {::initialize!
         (fn [{state :db}]
           {:db (-> state
                    (assoc
                     :create-group {:sending? false
                                    :error nil
                                    :validations validations
                                    :should-validate? false
                                    :fields (helpers/init-fields validations)}))
            :dispatch-n [[:braid.core.client.gateway.forms.user-auth.events/initialize! :register]
                         [:braid.group-create.core.events/validate-all]]})

         :braid.group-create.core.events/guess-group-url!
         (fn [{state :db} _]
           (let [group-name (get-in state [:create-group :fields :group-name :value])
                 group-url (get-in state [:create-group :fields :group-url :value])]
             {:db (if (string/blank? group-url)
                    (-> state
                        (assoc-in [:create-group :fields :group-url :value] (slugify group-name))
                        (assoc-in [:create-group :fields :group-url :untouched?] false))
                    state)
              :dispatch-n [[:braid.group-create.core.events/clear-errors :group-url]
                           [:braid.group-create.core.events/validate-field :group-url]]}))

         :braid.group-create.core.events/remote-create-group!
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
                        (dispatch [:braid.group-create.core.events/handle-registration-response! response]))
                      :on-error
                      (fn [error]
                        (when-let [k (get-in error [:response :error])]
                          (dispatch [:braid.group-create.core.events/handle-registration-error! k])))}})

         :braid.group-create.core.events/handle-registration-response!
         (fn [{state :db} [_ {:keys [group-id group] :as response}]]
           {:db (-> state
                    (assoc-in [:create-group :sending?] false)
                    (assoc-in [:groups group-id] group))

           ;; need to do a full redirect, because the sign-up flow via gateway.js
           ;; doesn't have any of the other assets
           ;; :redirect-to (routes/inbox-page-path {:group-id group-id})
            :dispatch [:braid.group-create.core.events/redirect! (routes/group-page-path {:group-id group-id
                                                                                          :page-id "inbox"})]})

         :braid.group-create.core.events/redirect!
         (fn [_ [_ url]]
           (set! js/window.location url)
           {})

         :braid.group-create.core.events/handle-registration-error!
         (fn [{state :db} [_ error]]
           {:db (-> state
                    (assoc-in [:create-group :sending?] false)
                    (assoc-in [:create-group :error] error))})})

       (helpers/reg-form-event-fxs :braid.group-create.core.events :create-group)

       (base/register-subs!
        {:braid.group-create.core.subs/sending?
         (fn [state _]
           (get-in state [:create-group :sending?]))

         :braid.group-create.core.subs/error
         (fn [state _]
           (get-in state [:create-group :error]))})

       (helpers/reg-form-subs :braid.group-create.core.subs :create-group)

       (base/register-system-page!
        {:key :create-group
         :view views/create-group-page-view
         :styles styles/>create-group-styles})

       (group-explore.api/register-link!
        {:title "Create Group"
         :url (routes/system-page-path {:page-id "create-group"})}))))
