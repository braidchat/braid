(ns braid.core.server.routes.helpers
  (:require
   [braid.chat.db.user :as user]
   [ring.middleware.anti-forgery :as anti-forgery]))

(defn logged-in? [req]
  (when-let [user-id (get-in req [:session :user-id])]
    (user/user-id-exists? user-id)))

(defn current-user [req]
  (when-let [user-id (get-in req [:session :user-id])]
    (when (user/user-id-exists? user-id)
      (user/user-by-id user-id))))

(defn current-user-id [req]
  (when-let [user-id (get-in req [:session :user-id])]
    (when (user/user-id-exists? user-id)
      user-id)))

(defn session-token []
  anti-forgery/*anti-forgery-token*)

(defn error-response [status msg]
  {:status status
   :headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str {:error msg})})

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})
