(ns braid.base.api
  (:require
    [braid.core.common.util :as util]
    #?@(:cljs
         [[braid.base.client.events]
          [braid.base.client.subs]
          [braid.base.client.pages]
          [braid.base.client.styles]
          [braid.base.client.state]
          [braid.base.client.remote-handlers]
          [braid.base.client.root-view]]
         :clj
         [[braid.base.conf]
          [braid.base.server.jobs]
          [braid.base.server.seed]
          [braid.base.server.cqrs]
          [braid.base.server.http-api-routes]
          [braid.base.server.initial-data]
          [braid.base.server.spa]
          [braid.base.server.schema]
          [braid.base.server.ws-handler]])))

#?(:cljs
   (do
     (defn register-initial-user-data-handler!
       "Add a handler that will run with the initial db & user-info recieved from the server. See `:register-initial-user-data` under `:clj`"
       [f]
       {:pre [(fn? f)]}
       (swap! braid.base.client.events/initial-user-data-handlers conj f))

     (defn register-state!
       "Add a key and initial value to the default app state, plus an associated spec."
       [state spec]
       {:pre [(map? state)
              (map? spec)]}
       (braid.base.client.state/register-state! state spec))

     (defn register-incoming-socket-message-handlers!
       "Registers multiple client-side socket message handlers.

       Expects map of event-keys and handler-fns.
       Handler fn will be called with user-id and data arguments"
       [handler-map]
       {:pre [(map? handler-map)
              (every? keyword? (keys handler-map))
              (every? fn? (vals handler-map))]}
       (swap! braid.base.client.remote-handlers/incoming-socket-message-handlers merge handler-map))

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

     (defn register-subs-raw!
       "Registers multiple re-frame subscription handlers, as if passed to reg-sub-raw.

       Expects a map of sub-keys to sub-handler-fns."
       [sub-map]
       {:pre [(map? sub-map)
              (every? keyword? (keys sub-map))
              (every? fn? (vals sub-map))]}
       (braid.base.client.subs/register-subs-raw! sub-map))

     (defn register-root-view!
       "Add a new view to the app (when user is logged in).
       Will be put under body > #app > .app > .main >"
       [view]
       {:pre [(fn? view)]}
       (swap! braid.base.client.root-view/root-views conj view))

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

     (defn register-initial-user-data!
       "Add a map of key -> fn for getting the initial user data to be sent to the client. `fn` will recieve the user-id as its argument. See `:register-initial-user-data-handler` under `:cljs`"
       [f]
       {:pre [(fn? f)]}
       (swap! braid.base.server.initial-data/initial-user-data conj f))

     (defn register-additional-script!
       "Add a javascript script tag to client html. Values can be a map with a `:src` or `:body` key or a function with no arguments, returing the same."
       [tag]
       {:pre [(util/valid? braid.base.server.spa/additional-script-dataspec tag)]}
       (swap! braid.base.server.spa/additional-scripts conj tag))

     (defn register-db-schema!
       "Add new datoms to the db schema"
       [schema]
       {:pre [(vector? schema)
              (every? (partial util/valid? braid.base.server.schema/rule-dataspec) schema)]}
       (swap! braid.base.server.schema/schema into schema))

     (defn register-db-seed-fn!
       "Will register function to call when base.server.seed/seed! is called"
       [f]
       {:pre [(fn? f)]}
       (swap! braid.base.server.seed/seed-fns conj f))

     (defn register-config-var!
       "Add a keyword to be read from `env` and added to the `config` state"
       [k]
       {:pre [(keyword? k)]}
       (swap! braid.base.conf/config-vars conj k))

     (defn register-server-message-handlers!
       "Add a map of websocket-event-name -> event-handler-fn to handle events from the client"
       [handler-defs]
       {:pre [(map? handler-defs)
              (every? keyword? (keys handler-defs))
              (every? fn? (vals handler-defs))]}
       (swap! braid.base.server.ws-handler/message-handlers merge handler-defs))

     (defn register-commands!
       "Add a command, which also exposes a websocket server-handler for a message of the same name."
       [commands]
       (swap! braid.base.server.cqrs/commands
              concat commands)

       (braid.base.server.cqrs/update-registry!)

       (register-server-message-handlers!
         (->> commands
              (map (fn [command]
                     [(:id command)
                      (braid.base.server.cqrs/->ws-handler command)]))
              (into {}))))

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
