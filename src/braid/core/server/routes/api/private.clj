(ns braid.core.server.routes.api.private
  (:require
   [braid.core.server.db :as db]
   [braid.chat.db.group :as group]
   [braid.chat.events :as events]
   [braid.base.server.cqrs :as cqrs]
   [braid.core.server.routes.helpers :as helpers :refer [error-response edn-response]]
   [braid.lib.markdown :refer [markdown->hiccup]]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [compojure.core :refer [GET PUT DELETE defroutes]]
   [taoensso.timbre :as timbre]))

(defroutes api-private-routes

  (GET "/csrf" req (edn-response {:token (helpers/session-token)}))

  ; get current logged in user
  (GET "/session" req
    (if-let [user (helpers/current-user req)]
      (edn-response {:user user
                     :csrf-token (helpers/session-token)})
      {:status 401 :body "" :session nil}))

  ; log out
  (DELETE "/session" _
    {:status 200 :session nil})

  ; create a group
  (PUT "/groups" [name slug type :as req]
    (let [group-id (db/uuid)]
      (cqrs/dispatch :braid.chat/create-group!
                     {:user-id (get-in req [:session :user-id])
                      :group-id group-id
                      :name name
                      :slug slug
                      :public? (case type
                                 "public" true
                                 "private" false)})
      (edn-response {:group-id group-id
                     :group (group/group-by-id group-id)})))

  (PUT "/groups/:group-id/join" [group-id :as req]
    (let [group-id (java.util.UUID/fromString group-id)]
      (cond
        ; logged in?
        (not (helpers/logged-in? req))
        (error-response 401 "Must be logged in.")

        ; public group?
        (not (:public? (group/group-by-id group-id)))
        (error-response 401 "Can only join public group via this endpoint")

        :else
        (do
          (events/user-join-group! (helpers/current-user-id req) group-id)
          (edn-response {:status "OK"})))))

  (GET "/changelog" []
    (edn-response {:braid/ok
                   (-> (io/resource "CHANGELOG.md")
                       slurp
                       markdown->hiccup)})))
