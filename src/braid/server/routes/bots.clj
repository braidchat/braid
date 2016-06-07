(ns braid.server.routes.bots
  (:require [clojure.string :as string]
            [compojure.core :refer [PUT defroutes]]
            [ring.middleware.transit :as transit]
            [taoensso.timbre :as timbre]
            [chat.server.db :as db]
            [braid.common.schema :as schema])
  (:import [org.apache.commons.codec.binary Base64]))

(defn basic-auth-req
  [request]
  (when-let [[user pass] (some-> (get-in request [:headers "authorization"])
                                 (->> (re-find #"^Basic (.*)$"))
                                 last
                                 Base64/decodeBase64
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
    (and (= (bot :group-id) (msg :group-id))
      (let [thread-group (db/thread-group-id (msg :thread-id))]
        (or (nil? thread-group) (= (bot :group-id) thread-group)))
      (every? (comp (partial = (msg :group-id)) db/tag-group-id)
              (msg :mentioned-tag-ids))
      (every? (fn [mentioned] (db/user-in-group? mentioned (msg :group-id)))
              (msg :mentioned-user-ids)))))

(defroutes bot-routes'
  ; TODO: allow updating/make this idempotent?
  (PUT "/message" req
    (let [bot-id (get req ::bot-id)
          msg (assoc (req :body)
                :user-id bot-id
                :created-at (java.util.Date.))]
      (if (schema/new-message-valid? msg)
        (if (bot-can-message? bot-id msg)
          (do
            ; TODO: who is the creator?
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

(def bad-transit-resp
  {:status 400
   :headers {"Content-Type" "text/plain"}
   :body "Malformed transit body"})

(def bot-routes
  (-> bot-routes'
      wrap-basic-auth
      (transit/wrap-transit-body {:keywords? true
                                  :malformed-response bad-transit-resp})))
