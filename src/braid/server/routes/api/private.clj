(ns braid.server.routes.api.private
  (:require [compojure.core :refer [GET defroutes]]
            [clojure.java.io :as io]
            [braid.server.db :as db]
            [braid.server.s3 :as s3]
            [braid.server.db.user :as user]
            [braid.server.api.embedly :as embedly]
            [braid.server.api.github :as github]
            [braid.server.markdown :refer [markdown->hiccup]]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})

(defroutes api-private-routes
  (GET "/changelog" []
    (edn-response {:braid/ok
                   (-> (io/resource "CHANGELOG.md")
                       slurp
                       markdown->hiccup)}))

  (GET "/extract" [url :as {session :session}]
    (if (user/user-id-exists? (:user-id session))
      (edn-response (embedly/extract url))
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
