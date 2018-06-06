(ns braid.core.server.routes.api.modules
  (:require
    [clout.core :as clout]
    [braid.core.hooks :as hooks]))

(defonce module-public-http-routes (hooks/register! (atom [])))

(defn- matches?
  [request {:keys [method clout-matcher]}]
  (and
    (or
      (= method :any)
      (= method (request :request-method)))
    (clout/route-matches clout-matcher request)))

(defn- dispatch
  [request routes]
  (->> routes
       (some (fn [route-meta]
               (let [handler-fn (route-meta :handler-fn)
                     params (clout/route-matches (route-meta :clout-matcher) request)]
                 (handler-fn (update request :params merge params)))))))

(defn- make-handler
  []
  (let [routes (->> @module-public-http-routes
                    (map (fn [[method url-pattern handler-fn]]
                           {:method method
                            :url-pattern url-pattern
                            :handler-fn handler-fn
                            :clout-matcher (clout/route-compile url-pattern)})))]
    (fn [request]
      (->> routes
           (filter (fn [route-meta]
                     (matches? request route-meta)))
           (dispatch request)))))

(defn dynamic-handler [request]
  ((make-handler) request))

; TODO
; the dynamic handler parses all the defined routes on each request
; this is fine in dev, b/c it allows for a reloaded workflow in the backend
; in prod, however, this is inefficient, and we should use a static-handler
; but, other modules would need to register their routes before starting the server
; but, changing the order of modules is not currently possible
; (def handler (make-handler))

(def handler dynamic-handler)

(defn valid-route?
  [[method url-pattern handler]]
  (and
    (keyword? method)
    (string? url-pattern)
    (fn? handler)))
