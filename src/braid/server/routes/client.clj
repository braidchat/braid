(ns braid.server.routes.client
  (:require
    [compojure.coercions :refer [as-uuid]]
    [compojure.core :refer [GET defroutes]]
    [compojure.route :refer [resources]]
    [clostache.parser :as clostache]
    [environ.core :refer [env]]
    [ring.util.response :refer [resource-response]]
    [braid.server.api.github :as github]
    [braid.server.conf :refer [config]]
    [braid.server.digest :as digest]
    [braid.server.db.group :as group]
    [braid.server.db.invitation :as invitation]
    [braid.server.db.user :as user]
    [braid.server.invite :as invites]))

(def prod-js? (= (env :environment) "prod"))

(defn get-html [client vars]
  (clostache/render-resource
    (str "public/" client ".html")
    (merge {:prod prod-js?
            :dev (not prod-js?)
            :algo "sha256"
            :js (if prod-js?
                  (str (digest/from-file (str "public/js/prod/" client ".js")))
                  (str (digest/from-file (str "public/js/dev/" client ".js"))))
            :basejs (when prod-js?
                      (str (digest/from-file (str "public/js/prod/base.js"))))
            :api_domain (config :api-domain)}
           vars)))

(defroutes desktop-client-routes

  ; public group page
  (GET "/group/:group-name" [group-name :as req]
    (if-let [group (group/public-group-with-name group-name)]
      (clostache/render-resource "templates/public_group_desktop.html.mustache"
                                 {:group-name (group :name)
                                  :group-id (group :id)
                                  :api-domain (config :api-domain)})
      {:status 403
       :headers {"Content-Type" "text/plain"}
       :body "No such public group"}))

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
        {:status 200 :headers {"Content-Type" "text/html"} :body (invites/link-signup-page group-id)}
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
      (clostache/render-resource
        "public/oauth-post-auth.html" {})
      ; else
      {:status 400
       :headers {"Content-Type" "text/plain"}
       :body "Incorrect provider"}))

  (GET "/gateway/create-group" []
    (get-html "gateway" {:gateway_action "create-group"}))

  (GET  "/gateway/request-password-reset" _
    (get-html "gateway" {:gateway_action "request-password-reset"}))

  (GET "/:slug" [slug]
    (when-let [group (group/group-by-slug slug)]
      {:status 302
       :headers {"Location" (str "/groups/" (group :id) "/inbox")}
       :body nil}))

  ; everything else
  (GET "/*" []
    (get-html "desktop" {})))

(defroutes mobile-client-routes
  ; TODO: add mobile routse for public joining & password resets
  (GET "/*" []
    (get-html "mobile" {})))

(defroutes resource-routes

  ; add cache-control headers to js files)
  ; (since it uses a cache-busted url anyway)
  (GET (str "/js/:build{desktop|mobile|gateway|base}.js") [build]
    (if prod-js?
      (if-let [response (resource-response (str "public/js/prod/" build ".js"))]
        (assoc-in response [:headers "Cache-Control"] "max-age=365000000, immutable")
        {:status 404
         :body "File Not Found"})
      (if-let [response (resource-response (str "public/js/dev/" build ".js"))]
        response
        {:status 200
         :headers {"Content-Type" "application/javascript"}
         :body (str "alert('The " build " js files are missing. Please compile them with cljsbuild or figwheel.');")})))

  (resources "/"))
