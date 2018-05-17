(ns braid.core.server.routes.socket
  (:require
   [braid.core.server.socket :refer [ring-ajax-post ring-ajax-get-or-ws-handshake]]
   [compojure.core :refer [GET POST defroutes]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defroutes sync-routes
  (GET  "/chsk" req
      (-> req
          (assoc-in [:session :ring.middleware.anti-forgery/anti-forgery-token]
            *anti-forgery-token*)
          (cond-> (and (not (get-in req [:session :user-id]))
                       (not (get-in req [:session :fake-id])))
            (assoc-in [:session :fake-id] (str "FAKE" (java.util.UUID/randomUUID))))
          ring-ajax-get-or-ws-handshake))
  (POST "/chsk" req (ring-ajax-post req)))
