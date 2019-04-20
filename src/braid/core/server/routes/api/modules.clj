(ns braid.core.server.routes.api.modules
  (:require
    [clout.core :as clout]
    [braid.core.hooks :as hooks]
    [braid.core.server.routes.helpers :as helpers]))

(defn route?
  [[method url-pattern handler]]
  (and
    (keyword? method)
    (string? url-pattern)
    (fn? handler)))

(defonce module-public-http-routes
  (hooks/register! (atom []) [route?]))

(defonce module-private-http-routes
  (hooks/register! (atom []) [route?]))

(defonce module-raw-http-routes
  (hooks/register! (atom []) [any?]))

;; XXX: the way this works means that routes with wrap-logged-in?
;; won't fall-through to other routes in the handler (because it
;; always returns something, even the route doesn't match).
;; In the future, investigate an alternate approach (e.g. wrapping the
;; individual routes with the authorization checking logic)
(defn- wrap-logged-in?
  "Return a 401 if the request is coming from a client that isn't logged in.

  WARNING: The route with this middleware must be the last in the
  handler, because it will swallow *all* requests it receives, even if
  the route doesn't match!"
  [handler]
  (fn [request]
    (if (helpers/logged-in? request)
      (handler request)
      {:status 401
       :body {:error "Must be logged in."}})))

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
  [routes-atom]
  (let [routes (->> @routes-atom
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

(defn- dynamic-handler
  [routes-atom]
  (fn [request]
    ((make-handler routes-atom) request)))

;; TODO
;; the dynamic handler parses all the defined routes on each request
;; this is fine in dev, b/c it allows for a reloaded workflow in the backend
;; in prod, however, this is inefficient, and we should use a static-handler
;; but, other modules would need to register their routes before starting the server
;; but, changing the order of modules is not currently possible
;; (def public-handler (make-handler module-public-http-routes))
;; (def private-handler (make-handler module-private-http-routes))

(def public-handler (dynamic-handler module-public-http-routes))
(def private-handler (-> (dynamic-handler module-private-http-routes)
                         wrap-logged-in?))
(def raw-handlers (dynamic-handler module-raw-http-routes))
