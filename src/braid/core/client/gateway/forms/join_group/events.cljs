(ns braid.core.client.gateway.forms.join-group.events
  (:require
   [braid.core.client.gateway.helpers :as helpers :refer [get-url-group-id]]
   [braid.base.client.state :refer [reg-event-fx]] ;; TODO should use reg.base.api/register-events!
   [clojure.string :as string]
   [re-frame.core :refer [dispatch]]))

(reg-event-fx
  ::initialize!
  (fn [{state :db}]
    {:dispatch-n [[:braid.core.client.gateway.forms.user-auth.events/initialize! :log-in]]}))

(reg-event-fx
  ::remote-join-group!
  (fn [{state :db} _]
    {:edn-xhr {:method :put
               :uri (str "/groups/" (get-url-group-id) "/join")
               :headers {"x-csrf-token" (state :csrf-token)}
               :on-complete
               (fn [response]
                 (set! js/window.location (str "/groups/" (get-url-group-id) "/inbox")))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [:braid.notices/display! [(keyword "join-group-error" (get-url-group-id)) k :error]])))}}))
