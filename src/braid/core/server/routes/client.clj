(ns braid.core.server.routes.client
  (:require
   [braid.chat.db.group :as group]
   [braid.chat.db.invitation :as invitation]
   [braid.chat.db.user :as user]
   [braid.core.server.invite :as invites]
   [braid.core.server.routes.helpers :as helpers]
   [braid.base.server.spa :as spa]
   [braid.lib.github :as github]
   [cljstache.core :as cljstache]
   [compojure.coercions :refer [as-uuid]]
   [compojure.core :refer [GET defroutes]]))

(defroutes desktop-client-routes

  ; invite accept page
  (GET "/accept" [invite :<< as-uuid tok]
    (if (and invite tok)
      (if-let [invite (invitation/invite-by-id invite)]
        {:status 200 :headers {"Content-Type" "text/html"} :body (invites/register-page invite tok)}
        {:status 400 :headers {"Content-Type" "text/plain"} :body "Invalid invite"})
      {:status 400 :headers {"Content-Type" "text/plain"} :body "Bad invite link, sorry"}))

  ; invite link
  (GET "/invite" [group-id :<< as-uuid nonce expiry mac]
    (if (and group-id nonce expiry mac)
      (if (invites/verify-hmac mac (str nonce group-id expiry))
        {:status 200 :headers {"Content-Type" "text/html"}
         :body (invites/link-signup-page group-id)}
        {:status 400 :headers {"Content-Type" "text/plain"} :body "Parameter verification failed"})
      {:status 400 :headers {"Content-Type" "text/plain"} :body "Missing required parameters"}))

  ; password reset page
  (GET "/reset" [user :<< as-uuid token :as req]
    (if-let [u (and user token
                 (invites/verify-reset-nonce {:id user} token)
                 (user/user-by-id user))]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (invites/reset-page u token)}
      {:status 401
       :headers {"Content-Type" "text/plain"}
       :body "Bad user or token"}))

  (GET "/gateway/oauth/:provider/auth" [provider]
    (case provider
      "github"
      {:status 302
       :headers {"Location" (github/build-authorize-link {})}}
      ; else
      {:status 400
       :headers {"Content-Type" "text/plain"}
       :body "Incorrect provider"}))

  (GET "/gateway/oauth/:provider/post-auth" [provider]
    (case provider
      "github"
      (cljstache/render-resource
        "public/oauth-post-auth.html" {})
      ; else
      {:status 400
       :headers {"Content-Type" "text/plain"}
       :body "Incorrect provider"}))

  (GET "/gateway/join-group/:group-id" [group-id]
    (spa/get-html "gateway" {:gateway_action "join-group"}))

  (GET "/gateway/create-group" []
    (spa/get-html "gateway" {:gateway_action "create-group"}))

  (GET "/gateway/request-password-reset" _
    (spa/get-html "gateway" {:gateway_action "request-password-reset"}))

  (GET "/:slug" [slug :as req]
    (when-let [group (group/group-by-slug slug)]
      (when (or (:public? group)
                (some-> (helpers/current-user req)
                        :group-ids
                        set
                        (contains? (group :id))))
        {:status 302
         :headers {"Location" (str "/groups/" (group :id) )}
         :body nil})))

  ; everything else
  (GET "/*" []
    (spa/get-html "desktop" {})))

