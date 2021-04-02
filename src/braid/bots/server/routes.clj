(ns braid.bots.server.routes
  (:require
    [braid.core.common.schema :as schema]
    [braid.core.server.db :as db]
    [braid.bots.server.db :as bot]
    [braid.chat.db.group :as group]
    [braid.chat.db.message :as message]
    [braid.chat.db.tag :as tag]
    [braid.chat.db.thread :as thread]
    [braid.chat.db.user :as user]
    [braid.core.server.sync-helpers :as sync-helpers]
    [braid.core.common.util :as util]
    [clojure.string :as string]
    [compojure.core :refer [GET POST PUT defroutes context]]
    [ring.middleware.transit :as transit]
    [taoensso.timbre :as timbre])
  (:import
   (org.apache.commons.codec.binary Base64)))

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
        (and (bot/bot-auth? bot-id pass) bot-id))
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
  (let [bot (bot/bot-by-id bot-id)]
    (cond
      (let [thread-group (thread/thread-group-id (msg :thread-id))]
        (and (some? thread-group) (not= (bot :group-id) thread-group)))
      (do (timbre/debugf "Bot %s attempted to send to a thread in a different group"
                         (bot :id))
          nil)

      (some (comp (partial not= (msg :group-id)) tag/tag-group-id)
            (msg :mentioned-tag-ids))
      (do (timbre/debugf "Bot %s attempted to add tag from other group" (bot :id))
          nil)

      (some (fn [mentioned] (not (group/user-in-group? mentioned (msg :group-id))))
            (msg :mentioned-user-ids))
      (do (timbre/debugf "Bot %s attempted to mention a user from a different group"
                         (bot :id))
          nil)

      :else true)))

(def -bot-routes
  [[:post "/message"
    (fn [req]
      (let [bot-id (get req ::bot-id)
            bot (bot/bot-by-id bot-id)
            msg (assoc (req :body)
                       :user-id (bot :user-id)
                       :group-id (bot :group-id)
                       :created-at (java.util.Date.))]
        (if (util/valid? schema/NewMessage msg)
          (if (bot-can-message? bot-id msg)
            (do
              (timbre/debugf "Creating message from bot: %s %s" bot-id msg)
              (db/run-txns! (message/create-message-txn msg))
              (sync-helpers/broadcast-thread (msg :thread-id) [])
              (sync-helpers/notify-users msg)
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
           ;; TODO: when we have clojure.spec, use that to explain failure
           :body "malformed message content"})))]
   ;; [TODO]: switch bots to using POST instead, this doesn't make sense as PUT
   ;; [XXX]: Deprecated, use POST route instead
   [:put "/message"
    (fn [req]
      (let [bot-id (get req ::bot-id)
            bot (bot/bot-by-id bot-id)
            msg (assoc (req :body)
                       :user-id (bot :user-id)
                       :group-id (bot :group-id)
                       :created-at (java.util.Date.))]
        (timbre/warnf "Bot %s hitting deprecated PUT message endpoint" bot-id)
        (if (util/valid? schema/NewMessage msg)
          (if (bot-can-message? bot-id msg)
            (do
              (timbre/debugf "Creating message from bot: %s %s" bot-id msg)
              (db/run-txns! (message/create-message-txn msg))
              (sync-helpers/broadcast-thread (msg :thread-id) [])
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
           ;; TODO: when we have clojure.spec, use that to explain failure
           :body "malformed message content"})))]

   [:get "/names/:user-id"
    (fn [{{:keys [user-id]} :params :as req}]
      (let [bot-id (get req ::bot-id)
            bot (bot/bot-by-id bot-id)]
        (if-let [user-id (try (java.util.UUID/fromString user-id)
                              (catch IllegalArgumentException _ nil))]
          (if (group/user-in-group? user-id (bot :group-id))
            {:status 200
             :headers {"Content-Type" "text/plain"}
             :body (:nickname (user/user-by-id user-id))}
            {:status 403
             :headers {"Content-Type" "text/plain"}
             :body "Can't lookup user in a different group"})
          {:status 400
           :headers {"Content-Type" "text/plain"}
           :body "Invalid user id"})))]

   ;; TODO: allow unsubscribed by sending DELETE
   [:put "/subscribe/:thread-id"
    (fn [{{:keys [thread-id]} :params :as req}]
      (let [bot-id (get req ::bot-id)
            bot (bot/bot-by-id bot-id)]
        (if-let [thread-id (try (java.util.UUID/fromString thread-id)
                                (catch IllegalArgumentException _ nil))]
          (if (= (bot :group-id) (thread/thread-group-id thread-id))
            (do
              (db/run-txns! (bot/bot-watch-thread-txn bot-id thread-id))
              {:status 201
               :headers {"Content-Type" "text/plain"}
               :body "ok"})
            (do
              (timbre/warnf "bot %s tried to add to thread in other group %s"
                            bot-id thread-id)
              {:status 403
               :headers {"Content-Type" "text/plain"}
               :body "Can't subscribe to a thread in a different group"}))
          {:status 400
           :headers {"Content-Type" "text/plain"}
           :body "Invalid thread id"})))]])

(defn bad-transit-resp-fn
  [ex req handler]
  (timbre/warnf "transit error: %s\nrequest: %s" ex req)
  {:status 400
   :headers {"Content-Type" "text/plain"}
   :body "Malformed transit body"})

(defn wrap-middleware
  [routes middlewares]
  (mapv (fn [[method route handler]]
          [method route
           (reduce (fn [handler middleware]
                     (middleware handler))
                   handler middlewares)])
        routes))

(defn with-context
  [routes ctx]
  (mapv (fn [[method route handler]]
          [method (str ctx route) handler])
        routes))

(def bot-routes
  (-> -bot-routes
      (with-context "/bots")
      (wrap-middleware
        [wrap-basic-auth
         #(transit/wrap-transit-body
            % {:keywords? true
               :malformed-response-fn bad-transit-resp-fn})])))
