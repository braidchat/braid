(ns braid.server.routes.socket
  (:require
    [compojure.core :refer [GET POST defroutes]]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [braid.server.socket :refer [ring-ajax-post
                                 ring-ajax-get-or-ws-handshake]]))

(defroutes sync-routes
  (GET  "/chsk" req
      (-> req
          (assoc-in [:session :ring.middleware.anti-forgery/anti-forgery-token]
            *anti-forgery-token*)
          ring-ajax-get-or-ws-handshake))
  (POST "/chsk" req (ring-ajax-post req)))
