(ns braid.base.api
  (:require
    [braid.core.common.util :as util]
    #?@(:cljs
         [[braid.base.client.events]
          [braid.base.client.subs]
          [braid.base.client.pages]
          [braid.base.client.styles]]
         :clj
         [[braid.base.conf]
          [braid.base.server.jobs]
          [braid.base.server.http-api-routes]])))

#?(:cljs
   (do
     (defn register-events!
       "Registers multiple re-frame event handlers, as if passed to reg-event-fx.

       Expects a map of event-keys to event-handler-fns."
       [event-map]
       {:pre [(map? event-map)
              (every? keyword? (keys event-map))
              (every? fn? (vals event-map))]}
       (braid.base.client.events/register-events! event-map))

     (defn register-event-listener!
       "Register a function to intercept re-frame events."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.base.client.events/event-listeners conj f))

     (defn register-subs!
       "Registers multiple re-frame subscription handlers, as if passed to reg-sub.

       Expects a map of sub-keys to sub-handler-fns."
       [sub-map]
       {:pre [(map? sub-map)
              (every? keyword? (keys sub-map))
              (every? fn? (vals sub-map))]}
       (braid.base.client.subs/register-subs! sub-map))

     (defn register-styles!
       "Add Garden CSS styles to the page styles"
       [styles]
       {:pre [(util/valid? braid.base.client.styles/style-dataspec styles)]}
       (swap! braid.base.client.styles/module-styles conj styles))

     (defn register-system-page!
       "Registers a system page with its own URL.

       Expects a map with the following keys:
         :key      keyword
         :on-load  (optional) function to call
                   when page is navigated to
         :on-exit  (optional) function to call
                   when page is navigated away from
         :view   reagent view fn
         :styles  (optional) garden styles for the page

       Link for page can be generated using:
        (braid.core.client.routes/system-page-path
             {:page-id __})"
       [page]
       {:pre [(util/valid? braid.base.client.pages/page-dataspec page)]}
       (swap! braid.base.client.pages/pages assoc (page :key) page)
       (when (page :styles)
         (register-styles!
           [:#app>.app>.main
            (page :styles)]))))

   :clj
   (do
     (defn register-config-var!
       "Add a keyword to be read from `env` and added to the `config` state"
       [k]
       {:pre [(keyword? k)]}
       (swap! braid.base.conf/config-vars conj k))

    (defn register-public-http-route!
       "Add a public HTTP route.
        Expects a route defined as:
        [:method \"pattern\" handler-fn]

        handler-fn will be passed a ring request object (with query-params and body-params in :params key)
        handler-fn should return a ring-compatible response (if it is a clojure data structure, it will be converted to edn or transit-json, based on the accepts header)
        ex.
        [:get \"/foo/:bar\" (fn [request]
                              {:status 200
                               :body (get-in request [:params :bar])})]"
       [route]
       {:pre [(util/valid? braid.base.server.http-api-routes/route? route)]}
       (swap! braid.base.server.http-api-routes/module-public-http-routes conj route))

     (defn register-private-http-route!
       "Add a private HTTP route (one that requires a user to be logged in).
        Expects a route defined as:
        [:method \"pattern\" handler-fn]

        handler-fn will be passed a ring request object (with query-params and body-params in :params key)
        handler-fn should return a ring-compatible response (if it is a clojure data structure, it will be converted to edn or transit-json, based on the accepts header)
        ex.
        [:get \"/foo/:bar\" (fn [request]
                              {:status 200
                               :body (get-in request [:params :bar])})]"
       [route]
       {:pre [(util/valid? braid.base.server.http-api-routes/route? route)]}
       (swap! braid.base.server.http-api-routes/module-private-http-routes conj route))

     (defn register-raw-http-handler!
       "Add an HTTP handler that expects to handle all its own middleware
        Expects a ring handler function

        handler-fn will be passed a ring request object (with query-params and body-params in :params key)
        handler-fn should return a ring-compatible response "
       [handler]
       (swap! braid.base.server.http-api-routes/module-raw-http-routes conj handler))

     (defn register-daily-job!
       "Add a recurring job that will run once a day. Expects a zero-arity function."
       [job-fn]
       {:pre [(fn? job-fn)]}
       (braid.base.server.jobs/register-daily-job! job-fn))))
