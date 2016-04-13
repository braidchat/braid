(ns chat.server.handler
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [compojure.core :refer [routes]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults
                                              secure-site-defaults site-defaults]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [get-header]]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojure.tools.nrepl.server :as nrepl]
            [chat.server.sync :as sync :refer [sync-routes]]
            [chat.server.routes :as routes
             :refer [desktop-client-routes
                     mobile-client-routes
                     api-private-routes
                     api-public-routes
                     resource-routes
                     extension-routes]]
            [chat.server.conf :as conf]
            [environ.core :refer [env]]
            ; just requiring to register multimethods
            chat.server.extensions.asana
            [chat.server.email-digest :as email-digest]))

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
      (def session-store (memory-store)))))

(defn assoc-cookie-conf [defaults]
  (-> defaults
      (assoc-in [:session :cookie-attrs :secure] (= (env :environment) "prod"))
      (assoc-in [:session :cookie-attrs :max-age] (* 60 60 24 7))
      (assoc-in [:session :store] session-store)))

(defn assoc-csrf-conf [defaults]
  (-> defaults
      (assoc-in [:security :anti-forgery]
        {:read-token (fn [req] (-> req :params :csrf-token))})))

(def static-site-defaults
  {:static {:resources "public"}
   :params {:urlencoded true
            :multipart  true
            :nested     true
            :keywordize true}
   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true
               :default-charset        "utf-8"}})

(def mobile-client-app
  (-> (routes
        resource-routes
        mobile-client-routes)
      (wrap-defaults static-site-defaults)))

(def desktop-client-app
  (-> (routes
        resource-routes
        desktop-client-routes)
      (wrap-defaults static-site-defaults)))

; XXX: Review use of CSRF
(def api-server-app
  (-> (routes
        (-> api-public-routes
            (wrap-defaults (-> site-defaults
                               (assoc-in [:security :anti-forgery] false)
                               assoc-cookie-conf)))

        (-> extension-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
      ;                         assoc-csrf-conf
                               )))
        (-> api-private-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
      ;                         assoc-csrf-conf
                               )))
        (-> sync-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
      ;                         assoc-csrf-conf
                               ))))
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-credentials true
                 :access-control-allow-methods [:get :put :post :delete])
      wrap-edn-params))

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
    (reset! conf/api-port api-port)
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
