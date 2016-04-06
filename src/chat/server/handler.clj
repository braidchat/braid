(ns chat.server.handler
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [compojure.route :refer [resources]]
            [compojure.core :refer [GET POST routes defroutes context]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults
                                              secure-site-defaults site-defaults]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.tools.nrepl.server :as nrepl]
            [clostache.parser :as clostache]
            [chat.server.sync :as sync :refer [sync-routes]]
            [environ.core :refer [env]]
            [chat.shared.util :refer [valid-nickname?]]
            [chat.server.cache :refer [random-nonce]]
            [chat.server.db :as db]
            [chat.server.invite :as invites]
            [chat.server.digest :as digest]
            [chat.server.s3 :as s3]
            [chat.server.extensions :as ext :refer [b64->str]]
            ; just requiring to register multimethods
            chat.server.extensions.asana
            [chat.server.email-digest :as email-digest]
            [chat.server.identicons :as identicons]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})

(defn get-html [client]
  (let [replacements {:algo "sha256"
                      :js (str (digest/from-file (str "/js/" client "/out/braid.js")))
                      ; TODO don't hardcode this
                      :api_domain "localhost:5557"}]
    (clostache/render-resource (str "public/" client ".html") replacements)))

(defroutes desktop-client-routes
  (GET "/*" []
    (get-html "desktop")))

(defroutes mobile-client-routes
  (GET "/*" []
    (get-html "mobile")))

(defroutes api-server-routes
  (GET "/accept" [invite tok]
    (if (and invite tok)
      (if-let [invite (db/with-conn (db/get-invite (java.util.UUID/fromString invite)))]
        {:status 200 :headers {"Content-Type" "text/html"} :body (invites/register-page invite tok)}
        {:status 400 :headers {"Content-Type" "text/plain"} :body "Invalid invite"})
      {:status 400 :headers {"Content-Type" "text/plain"} :body "Bad invite link, sorry"}))

  (POST "/public-register/:group-id" [group-id email :as req]
    (let [group-id (java.util.UUID/fromString group-id)
          group-settings (db/with-conn (db/group-settings group-id))]
      (if-not (get group-settings :public?)
        {:status 400 :body "No such group or the group is private"}
        (if (string/blank? email)
          {:status 400 :body "Invalid email"}
          (let [id (db/uuid)
                avatar (identicons/id->identicon-data-url id)
                ; XXX: copied from chat.shared.util/nickname-rd
                disallowed-chars #"[ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]"
                nick (-> (first (string/split email #"@"))
                         (string/replace disallowed-chars ""))
                u (db/with-conn (db/create-user! {:id id
                                                  :email email
                                                  :password (random-nonce 50)
                                                  :avatar avatar
                                                  :nickname nick}))]
            (db/with-conn
              (db/user-add-to-group! id group-id)
              (db/user-subscribe-to-group-tags! id group-id))
            (sync/broadcast-user-change id [:chat/new-user u])
            {:status 302 :headers {"Location" "/"}
             :session (assoc (req :session) :user-id id)
             :body ""})))))

  (POST "/register" [token invite_id password email now hmac nickname avatar :as req]
    (let [fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? password) (assoc fail :body "Must provide a password")

        (not (invites/verify-hmac hmac (str now token invite_id email)))
        (assoc fail :body "Invalid HMAC")

        (string/blank? invite_id) (assoc fail :body "Invalid invitation ID")

        (not (valid-nickname? nickname))
        (assoc fail :body "Nickname must be 1-30 characters without whitespace")

        (db/with-conn (db/nickname-taken? nickname))
        (assoc fail :body "nickname taken")

        ; TODO: be smarter about this
        (not (#{"image/jpeg" "image/png"} (:content-type avatar)))
        (assoc fail :body "Invalid image")

        :else
        (let [invite (db/with-conn (db/get-invite (java.util.UUID/fromString invite_id)))]
          (if-let [err (:error (invites/verify-invite-nonce invite token))]
            (assoc fail :body "Invalid invite token")
            (let [avatar-url (invites/upload-avatar avatar)
                  user (db/with-conn (db/create-user! {:id (db/uuid)
                                                       :email email
                                                       :avatar avatar-url
                                                       :nickname nickname
                                                       :password password}))]
              (db/with-conn
                (db/user-add-to-group! (user :id) (invite :group-id))
                (db/user-subscribe-to-group-tags! (user :id) (invite :group-id))
                (db/retract-invitation! (invite :id)))
              (sync/broadcast-user-change (user :id) [:chat/new-user user])
              {:status 302 :headers {"Location" "/"}
               :session (assoc (req :session) :user-id (user :id))
               :body ""}))))))

  (POST "/logout" req
    {:status 200 :session nil}))

(defroutes extension-routes
  (context "/extension" _
    (GET "/oauth" [state code]
      (let [{ext-id :extension-id} (-> state b64->str edn/read-string)
            ext (db/with-conn (db/extension-by-id ext-id))]
        (if ext
          (do (ext/handle-oauth-token ext state code)
              {:status 302
               :headers {"Location" (str "/" (ext :group-id))}
               :body ""})
          {:status 400 :body "No such extension"})))
    (POST "/webhook/:ext" [ext :as req]
      (if-let [ext (db/with-conn (db/extension-by-id (java.util.UUID/fromString ext)))]
        (ext/handle-webhook ext req)
        {:status 400 :body "No such extension"}))
    (POST "/config" [extension-id data]
      (if-let [ext (db/with-conn (db/extension-by-id (java.util.UUID/fromString extension-id)))]
        (ext/extension-config ext data)
        {:status 400 :body "No such extension"}))))

(defroutes api-routes
  (GET "/s3-policy" req
    (if (some? (db/with-conn (db/user-by-id (get-in req [:session :user-id]))))
      (if-let [policy (s3/generate-policy)]
        {:status 200
         :headers {"Content-Type" "application/edn"}
         :body (pr-str policy)}
        {:status 500
         :headers {"Content-Type" "application/edn"}
         :body (pr-str {:error "No S3 secret for upload"})})
      {:status 403
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:error "Unauthorized"})}))
  (POST "/auth" req
    (if-let [user-id (let [{:keys [email password]} (req :params)]
                       (when (and email password)
                         (db/with-conn (db/authenticate-user email password))))]
      {:status 200 :session (assoc (req :session) :user-id user-id)}
      {:status 401 :body (pr-str {:error true})}))
  (POST "/request-reset" [email]
    (when-let [user (db/with-conn (db/user-with-email email))]
      (invites/request-reset (assoc user :email email)))
    {:status 200 :body (pr-str {:ok true})})
  (GET "/reset" [user token]
    (if-let [u (and (invites/verify-reset-nonce user token)
                 (db/with-conn (db/user-by-id (java.util.UUID/fromString user))))]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (invites/reset-page u token)}
      {:status 401}))
  (POST "/reset" [new_password token user_id now hmac :as req]
    (let [user-id (java.util.UUID/fromString user_id)
          fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? new_password) (assoc fail :body "Must provide a password")

        (not (invites/verify-hmac hmac (str now token user-id)))
        (assoc fail :body "Invalid HMAC")

        :else
        (if-let [user (db/with-conn (db/user-by-id user-id))]
          (if-let [err (:error (invites/verify-reset-nonce user token))]
            (assoc fail :body err)
            (do (db/with-conn (db/set-user-password! (user :id) new_password))
                {:status 301
                 :headers {"Location" "/"}
                 :session (assoc (req :session) :user-id (user :id))
                 :body ""}))
          (assoc fail :body "Invalid user"))))))

(defroutes resource-routes
  (resources "/"))

(if (= (env :environment) "prod")
  (do
    (require 'taoensso.carmine.ring)
    (def ^:dynamic *redis-conf* {:pool {}
                                 :spec {:host "127.0.0.1"
                                        :port 6379}})
    (let [carmine-store (ns-resolve 'taoensso.carmine.ring 'carmine-store)]
      (def session-store
        (carmine-store '*redis-conf* {:expiration-secs (* 60 60 24 7)
                                      :key-prefix "braid"}))))
  (do
    (require 'ring.middleware.session.memory)
    (let [memory-store (ns-resolve 'ring.middleware.session.memory 'memory-store)]
      (def session-store
        (memory-store)))))

(def middleware-defaults
  (-> site-defaults ; ssl stuff will be handled by nginx
      (assoc-in [:session :cookie-attrs :secure] (= (env :environment) "prod"))
      (assoc-in [:session :store] session-store)
      (assoc-in [:security :anti-forgery]
        {:read-token (fn [req] (-> req :params :csrf-token))})))

(def mobile-client-app
  (routes
    (wrap-defaults
      (routes
        resource-routes
        mobile-client-routes)
      middleware-defaults)))

(def desktop-client-app
  (routes
    (wrap-defaults
      (routes
        resource-routes
        desktop-client-routes)
      middleware-defaults)))

(def api-server-app
  (->
    (routes
      (wrap-defaults
        (routes
          api-routes
          sync-routes
          extension-routes)
        (-> api-defaults
            (assoc-in [:session :cookie-attrs :secure] (= (env :environment) "prod"))
            (assoc-in [:session :store] session-store)))
      (-> (routes
             resource-routes
             api-server-routes)
          (wrap-defaults middleware-defaults)
          ))
    wrap-edn-params
    (wrap-cors :access-control-allow-origin [#".*"]
               :access-control-allow-headers ["Origin" "X-Requested-With"
                                              "Content-Type" "Accept"]
               :access-control-allow-methods [:get :put :post :delete])
    ))

;; server
(defonce servers (atom {:api nil
                        :mobile nil
                        :desktop nil}))

(defn stop-server!
  [type]
  (when-let [stop-fn (@servers type)]
    (stop-fn :timeout 100)))

(defn start-server!
  [type port]
  (stop-server! type)
  (let [app (case type
              :api #'api-server-app
              :desktop #'desktop-client-app
              :mobile #'mobile-client-app)]
    (swap! servers assoc type (run-server app {:port port}))))

(defn start-servers! [port]
  (let [desktop-port port
        mobile-port (+ 1 port)
        api-port (+ 2 port)]
    (println "starting desktop client on port " desktop-port)
    (start-server! :desktop desktop-port)
    (println "starting mobile client on port " mobile-port)
    (start-server! :mobile mobile-port)
    (println "starting api on port " api-port)
    (start-server! :api api-port)))

;; scheduler
(defonce scheduler (atom nil))

(defn stop-scheduler!
  []
  (when-let [s @scheduler]
    (qs/shutdown s)))

(defn start-scheduler!
  []
  (stop-scheduler!)
  (reset! scheduler (qs/initialize))
  (qs/start @scheduler))

;; main
(defn -main  [& args]
  (let [port (Integer/parseInt (first args))
        repl-port (Integer/parseInt (second args))]
    (start-servers! port)
    (chat.server.sync/start-router!)
    (nrepl/start-server :port repl-port)
    (println "starting quartz scheduler")
    (start-scheduler!)
    (println "scheduling email digest jobs")
    (email-digest/add-jobs @scheduler)))
