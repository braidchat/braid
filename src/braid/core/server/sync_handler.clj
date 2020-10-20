(ns braid.core.server.sync-handler
  (:require
   [braid.core.hooks :as hooks]
   [braid.core.server.sync-helpers :as helpers]
   [taoensso.timbre :as timbre :refer [debugf]]
   [braid.chat.db.group :as group]
   [braid.chat.db.thread :as thread]
   ;; FIXME: should do these view base.api/something
   [braid.base.server.ws-handler :refer [anon-msg-handler
                                         event-msg-handler]]))

;; anonymous event handlers

(defonce anonymous-load-group (hooks/register! (atom []) [fn?]))

(defmethod anon-msg-handler :braid.server.anon/load-group
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [group (group/group-by-id ?data)]
    (when (:public? group)
      (?reply-fn (reduce (fn [m f] (f (group :id) m))
                         {:tags (group/group-tags ?data)
                          :group group
                          :threads (thread/public-threads ?data)}
                         @anonymous-load-group))
      (helpers/add-anonymous-reader ?data (get-in ev-msg [:ring-req :session :fake-id])))))

(defmethod anon-msg-handler :chsk/uidport-close
  [ev-msg]
  (debugf "Closing connection for anonymous client %s" (:client-id ev-msg))
  (helpers/remove-anonymous-reader (get-in ev-msg [:ring-req :session :fake-id])))

(defmethod anon-msg-handler :braid.client/ping
  [{:as ev-msg :keys [?reply-fn]}]
  (when-let [reply ?reply-fn]
    (reply [:braid.server/pong])))

;; logged in event handlers

(defmethod event-msg-handler :braid.client/ping
  [{:as ev-msg :keys [?reply-fn]}]
  (when-let [reply ?reply-fn]
    (reply [:braid.server/pong])))
