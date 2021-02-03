(ns braid.base.core
  (:require
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.base.client.router :as router]
          [braid.base.client.events :as events]
          [braid.base.client.socket :as socket]
          [re-frame.core :refer [dispatch]]]
         :clj
         [[braid.base.server.initial-data :as initial-data]])))

(defn init! []
  #?(:cljs
     (do
       (base/register-incoming-socket-message-handlers!
         {::init-data
          (fn [_ data]
            (dispatch [::set-init-data! data])
            (router/dispatch-current-path!)
            (dispatch [:notify-if-client-out-of-date! (data :version-checksum)]))

          :socket/connected
          (fn [_ _]
            (socket/chsk-send! [::server-start nil]))})

       (base/register-events!
         {::set-init-data!
          (fn [{state :db} [_ data]]
            ;; FIXME :set-login-state! defined in chat
            {:dispatch-n (list [:set-login-state! :app])
             :db (-> state
                     (as-> <>
                       (reduce (fn [db f] (f db data))
                               <>
                               @events/initial-user-data-handlers)))})}))

     :clj
     (do
       ;; TODO transfer appropriate vars from braid.chat.core
       (doseq [k [:app-title
                  :prod-js
                  :redis-uri]]
         (base/register-config-var! k))

       (base/register-server-message-handlers!
         {::server-start
          (fn [{:keys [user-id]}]
            {:chsk-send! [user-id [::init-data
                                   (->> @initial-data/initial-user-data
                                        (into {} (map (fn [f] (f user-id)))))]]})}))))
