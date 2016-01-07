(ns chat.server.handler
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [compojure.route :refer [resources]]
            [compojure.core :refer [GET POST routes defroutes context]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults secure-site-defaults site-defaults]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [clojure.string :as string]
            [clojure.tools.nrepl.server :as nrepl]
            [taoensso.carmine.ring :as carmine]
            [chat.server.sync :refer [sync-routes]]
            [environ.core :refer [env]]
            [chat.server.db :as db]
            [chat.server.invite :as invites]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8" }
   :body (pr-str clj-body)})

(defroutes site-routes
  (GET "/" []
    (-> "public/index.html"
        clojure.java.io/resource
        slurp))
  (GET "/accept" [invite tok]
    (if (and invite tok)
      (if-let [invite (db/with-conn (db/get-invite (java.util.UUID/fromString invite)))]
        {:status 200 :headers {"Content-Type" "text/html"} :body (invites/register-page invite tok)}
        {:status 400 :headers {"Content-Type" "text/plain"} :body "Invalid invite"})
      {:status 400 :headers {"Content-Type" "text/plain"} :body "Bad invite link, sorry"}))
  (POST "/register" [token invite_id password email now hmac avatar :as req]
    (let [fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? password) (assoc fail :body "Must provide a password")
        (not (invites/verify-hmac hmac (str now token invite_id email))) (assoc fail :body "Invalid HMAC")
        (string/blank? invite_id) (assoc fail :body "Invalid invitation ID")
        ; TODO: be smarter about this
        (not (#{"image/jpeg" "image/png"} (:content-type avatar))) (assoc fail :body "Invalid image")

        :else
        (let [invite (db/with-conn (db/get-invite (java.util.UUID/fromString invite_id)))]
          (if-let [err (:error (invites/verify-invite-nonce invite token))]
            (assoc fail :body "Invalid invite token")
            (let [avatar-url (invites/upload-avatar avatar)
                  user (db/with-conn (db/create-user! {:id (db/uuid)
                                                       :email email
                                                       :avatar avatar-url
                                                       :password password}))]
              (db/with-conn
                (db/user-add-to-group! (user :id) (invite :group-id))
                (db/user-subscribe-to-group-tags! (user :id) (invite :group-id))
                (db/retract-invitation! (invite :id)))
              {:status 302 :headers {"Location" "/"}
               :session (assoc (req :session) :user-id (user :id))
               :body ""}))))))

  (POST "/logout" req
    {:status 200 :session nil}))

(defroutes api-routes
  (POST "/auth" req
    (if-let [user-id (let [{:keys [email password]} (req :params)]
                       (when (and email password)
                         (db/with-conn (db/authenticate-user email password))))]
      {:status 200 :session (assoc (req :session) :user-id user-id)}
      {:status 401})))

(defroutes resource-routes
  (resources "/"))

(def ^:dynamic *redis-conf* {:pool {}
                             :spec {:host "127.0.0.1"
                                    :port 6379}})

(def app
  (->
    (routes
      (wrap-defaults
        (routes sync-routes api-routes)
        (-> api-defaults
            (assoc-in [:session :cookie-attrs :secure] (= (env :environment) "prod"))
            (assoc-in [:session :store] (carmine/carmine-store *redis-conf*
                                                               {:expiration-secs (* 60 60 24 7)
                                                                :key-prefix "lpchat"}))))
      (wrap-defaults
        (routes
          resource-routes
          site-routes)
        (-> site-defaults ; ssl stuff will be handled by nginx
            (assoc-in [:session :cookie-attrs :secure] (= (env :environment) "prod"))
            (assoc-in [:session :store] (carmine/carmine-store *redis-conf*
                                                               {:expiration-secs (* 60 60 24 7)
                                                                :key-prefix "lpchat"}))
            (assoc-in [:security :anti-forgery]
              {:read-token (fn [req] (-> req :params :csrf-token))}))))
    wrap-edn-params))

(defonce server (atom nil))

(defn stop-server!
  []
  (when-let [stop-fn @server]
    (stop-fn :timeout 100)))

(defn start-server!
  [port]
  (stop-server!)
  (reset! server (run-server #'app {:port port})))

(defn -main  [& args]
  (let [port (Integer/parseInt (first args))
        repl-port (Integer/parseInt (second args))]
    (start-server! port)
    (chat.server.sync/start-router!)
    (nrepl/start-server :port repl-port)
    (println "starting on port " port)))


