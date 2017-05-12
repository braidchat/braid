(ns braid.server.routes.api.private
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [compojure.core :refer [GET PUT DELETE defroutes]]
    [braid.server.db :as db]
    [braid.server.s3 :as s3]
    [braid.server.db.group :as group]
    [braid.server.db.user :as user]
    [braid.server.api.link-extract :as link-extract]
    [braid.server.api.github :as github]
    [braid.server.markdown :refer [markdown->hiccup]]
    [braid.server.routes.helpers :refer [current-user current-user-id logged-in?
                                         error-response edn-response
                                         session-token]]))

(defroutes api-private-routes

  ; get current logged in user
  (GET "/session" req
    (if-let [user (current-user req)]
      (edn-response {:user user
                     :csrf-token (session-token)})
      {:status 401 :body "" :session nil}))

  ; log out
  (DELETE "/session" _
    {:status 200 :session nil})

  ; create a group
  (PUT "/groups" [name slug type :as req]
    (cond
      ; logged in?
      (not (logged-in? req))
      (error-response 401 "Must be logged in.")

      ; group name validations

      (string/blank? name)
      (error-response 400 "Must provide a Group Name")

      ; group url (slug) validations

      (string/blank? slug)
      (error-response 400 "Must provide an Group URL")

      (not (re-matches #"[a-z0-9-]+" slug))
      (error-response 400 "Group URL can only contain lowercase letters, numbers or dashes.")

      (string/starts-with? "-" slug)
      (error-response 400 "Group URL cannot start with a dash.")

      (string/ends-with? "-" slug)
      (error-response 400 "Group URL cannot end with a dash.")

      (group/group-with-slug-exists? slug)
      (error-response 400 "A Group with this URL already exists.")

      ; group type validations

      (string/blank? type)
      (error-response 400 "Must provide a Group Type")

      (not (contains? #{"public" "private"} type))
      (error-response 400 "Group type must be either public or private")

      ; passed all validations
      :else
      (let [user-id (current-user-id req)
            group-id (db/uuid)
            [group] (db/run-txns!
                      (group/create-group-txn {:id group-id
                                               :slug slug
                                               :name name}))]
        (db/run-txns! (group/group-set-txn (group :id) :public? (case type
                                                                  "public" true
                                                                  "private" false)))
        (db/run-txns! (group/user-add-to-group-txn user-id group-id))
        (db/run-txns! (group/user-subscribe-to-group-tags-txn user-id group-id))
        (db/run-txns! (group/user-make-group-admin-txn user-id group-id))
        (edn-response {:group-id group-id}))))

  (GET "/changelog" []
    (edn-response {:braid/ok
                   (-> (io/resource "CHANGELOG.md")
                       slurp
                       markdown->hiccup)}))

  (GET "/extract" [url :as {session :session}]
    (if (user/user-id-exists? (:user-id session))
      (edn-response (link-extract/extract url))
      {:status 403
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:error "Unauthorized"})}))

  (GET "/s3-policy" req
    (if (user/user-id-exists? (get-in req [:session :user-id]))
      (if-let [policy (s3/generate-policy)]
        {:status 200
         :headers {"Content-Type" "application/edn"}
         :body (pr-str policy)}
        {:status 500
         :headers {"Content-Type" "application/edn"}
         :body (pr-str {:error "No S3 secret for upload"})})
      {:status 403
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:error "Unauthorized"})})))
