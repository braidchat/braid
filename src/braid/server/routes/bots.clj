(ns braid.server.routes.bots
  (:require [clojure.string :as string]
            [compojure.core :refer [PUT defroutes]]
            [ring.middleware.transit :as transit]
            [taoensso.timbre :as timbre]
            [braid.server.db :as db]
            [braid.common.schema :as schema]
            [braid.server.sync :as sync])
  (:import [org.apache.commons.codec.binary Base64]))

(defn basic-auth-req
  [request]
  (when-let [[user pass] (some-> (get-in request [:headers "authorization"])
                                 (->> (re-find #"^Basic (.*)$"))
                                 last
                                 Base64/decodeBase64
                                 (String.)
                                 (string/split #":" 2))]
    (try
      (let [bot-id (java.util.UUID/fromString user)]
        (and (db/bot-auth? bot-id pass) bot-id))
      (catch IllegalArgumentException e
        nil))))

(defn wrap-basic-auth
  [app]
  (fn [req]
    (if-let [bot-id (basic-auth-req req)]
      (app (assoc req ::bot-id bot-id))
      {:status 401
       :headers {"Content-Type" "text/plain; charset=utf-8"
                 "WWW-Authenticate" "Basic realm=\"braid chatbots\""}
       :body "bad credentials"})))

; TODO: when using clojure.spec, use spec to validate this
(defn bot-can-message?
  [bot-id msg]
  (let [bot (db/bot-by-id bot-id)]
    (cond
      (not= (bot :group-id) (msg :group-id))
      (do
        (timbre/debugf "Bot %s attempted to send a message to a group it isn't in %s"
                       (bot :group-id) (msg :group-id))
        nil)

      (let [thread-group (db/thread-group-id (msg :thread-id))]
        (and (some? thread-group) (not= (bot :group-id) thread-group)))
      (do (timbre/debugf "Bot %s attempted to send to a thread in a different group"
                         (bot :id))
          nil)

      (some (comp (partial not= (msg :group-id)) db/tag-group-id)
            (msg :mentioned-tag-ids))
      (do (timbre/debugf "Bot %s attempted to add tag from other group" (bot :id))
          nil)

      (some (fn [mentioned] (not (db/user-in-group? mentioned (msg :group-id))))
            (msg :mentioned-user-ids))
      (do (timbre/debugf "Bot %s attempted to mention a user from a different group"
                         (bot :id))
          nil)

      :else true)))

(defroutes bot-routes'
  ; TODO: allow updating/make this idempotent?
  (PUT "/message" req
    (let [bot-id (get req ::bot-id)
          bot (db/bot-by-id bot-id)
          msg (assoc (req :body)
                :user-id (bot :user-id)
                :created-at (java.util.Date.))]
      (if (schema/new-message-valid? msg)
        (if (bot-can-message? bot-id msg)
          (do
            (timbre/debugf "Creating message from bot: %s %s" bot-id msg)
            (db/create-message! msg)
            (sync/broadcast-thread (msg :thread-id) [])
            {:status 201
             :headers {"Content-Type" "text/plain"}
             :body "ok"})
          (do
            (timbre/debugf "bot %s tried to create illegal message %s"
                           bot-id msg)
            {:status 403
             :headers {"Content-Type" "text/plain"}
             :body "not allowed to do that"}))
        {:status 400
         :headers {"Content-Type" "text/plain"}
         ; TODO: when we have clojure.spec, use that to explain failure
         :body "malformed message content"}))))

(defn bad-transit-resp-fn
  [ex req handler]
  (println "transit error: " ex)
  (println "req" req)
  {:status 400
   :headers {"Content-Type" "text/plain"}
   :body "Malformed transit body"})

(def bot-routes
  (-> bot-routes'
      wrap-basic-auth
      (transit/wrap-transit-body {:keywords? true
                                  :malformed-response-fn bad-transit-resp-fn})))
