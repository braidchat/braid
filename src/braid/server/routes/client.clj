(ns braid.server.routes.client
  (:require
    [compojure.coercions :refer [as-uuid]]
    [compojure.core :refer [GET defroutes]]
    [compojure.route :refer [resources]]
    [clostache.parser :as clostache]
    [ring.util.response :refer [resource-response]]
    [braid.server.api.github :as github]
    [braid.server.conf :refer [config]]
    [braid.server.digest :as digest]
    [braid.server.db.group :as group]
    [braid.server.db.invitation :as invitation]
    [braid.server.db.user :as user]
    [braid.server.invite :as invites]))

(defn get-html [client vars]
  (clostache/render-resource
    (str "public/" client ".html")
    (merge {:algo "sha256"
            :js (str (digest/from-file (str "public/js/" client "/out/braid.js")))
            :api_domain (config :api-domain)}
           vars)))

(defroutes desktop-client-routes

  ; used by forward-proxy for domain redirect
  ; ie. slug.domain.com
  ;       -> www.domain.com/groups/slug
  ;       -> www.domain.com/group-uuid
  (GET "/groups/:slug" [slug]
    (if-let [group (group/group-by-slug slug)]
      {:status 302
       :headers {"Location" (str "/" (group :id) "/inbox")}
       :body nil}
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body "No such group exists."}))

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

  (GET "/github-login" []
    {:status 302
     :headers {"Location" (github/build-authorize-link {:register? false})}})

  (GET "/github-register" [group]
    {:status 302
     :headers
     {"Location"
      (github/build-authorize-link {:register? true
                                    :group-id (java.util.UUID/fromString group)})}})

  (GET "/gateway/create-group" []
    (get-html "gateway" {:gateway_action "create-group"}))

  (GET "/gateway/log-in" []
    (get-html "gateway" {:gateway_action "log-in"}))

  ; everything else
  (GET "/*" []
    (get-html "desktop" {})))

(defroutes mobile-client-routes
  ; TODO: add mobile routse for public joining & password resets
  (GET "/*" []
    (get-html "mobile" {})))

(defroutes resource-routes
  ; add cache-control headers to perma-cache braid.js
  ; (since it uses a cache-busted url anyway)

  (GET "/js/desktop/out/braid.js" []
    (if-let [response (resource-response "public/js/desktop/out/braid.js")]
     (assoc-in response [:headers "Cache-Control"] "max-age=365000000, immutable")
     {:status 200
      :headers {"Content-Type" "application/javascript"}
      :body "alert('The desktop js files are missing. please compile them with cljsbuild or figwheel.');"}))

  (GET "/js/mobile/out/braid.js" []
    (if-let [response (resource-response "public/js/mobile/out/braid.js")]
      (assoc-in response [:headers "Cache-Control"] "max-age=365000000, immutable")
      {:status 200
       :headers {"Content-Type" "application/javascript"}
       :body "alert('The mobile js files are missing. Please compile them with cljsbuild or figwheel.');"}))

  (resources "/"))
