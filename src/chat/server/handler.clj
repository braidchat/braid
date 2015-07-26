(ns chat.server.handler
  (:require [org.httpkit.server :refer [run-server]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [compojure.core :refer [GET POST routes defroutes context]]
            [ring.middleware.defaults :refer [wrap-defaults secure-site-defaults site-defaults]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [taoensso.carmine.ring :as carmine]
            [chat.server.sync :refer [sync-routes]]
            [environ.core :refer [env]]
            [chat.server.db :as db]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8" }
   :body (pr-str clj-body)})

(defroutes site-routes
  (GET "/" []
    (-> "chat.html"
        (clojure.java.io/resource)
        (clojure.java.io/file)
        (slurp)))
  (POST "/auth" req
    (if-let [user-id (let [{:keys [email password]} (req :params)]
                       (db/with-conn (db/authenticate-user email password)))]
      {:status 200 :session (assoc (req :session) :user-id user-id)}
      {:status 401}))
  (POST "/logout" req
    {:status 200 :session nil}))

(defroutes resource-routes
  (resources "/"))

(def ^:dynamic *redis-conf* {:pool {}
                             :spec {:host "127.0.0.1"
                                    :port 6379}})

(def app
  (->
    (wrap-defaults
      (routes
        resource-routes
        site-routes
        sync-routes)
      (-> (if (= (env :environment) "prod")
            secure-site-defaults
            site-defaults)
          (assoc-in [:session :store] (carmine/carmine-store *redis-conf*
                                                             {:expiration-secs (* 60 60 24 7)
                                                              :key-prefix "lpchat"}))
          (assoc-in [:security :anti-forgery]
            {:read-token (fn [req] (-> req :params :csrf-token))})))
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
  (let [port (Integer/parseInt (first args))]
    (start-server! port)
    (println "starting on port " port)))


