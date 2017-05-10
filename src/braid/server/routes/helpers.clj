(ns braid.server.routes.helpers
  (:require
    [braid.server.db.user :as user]))

(defn current-user [req]
  (when-let [user-id (get-in req [:session :user-id])]
    (when (user/user-id-exists? user-id)
      (user/user-by-id user-id))))

(defn error-response [status msg]
  {:status status
   :headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str {:error msg})})

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})
