(ns braid.core.api
  (:require
    [braid.core.common.util :as util]
    #?@(:cljs
         [[braid.core.client.ui.views.new-message]
          [braid.core.client.ui.views.message]
          [braid.core.client.state]
          [braid.core.client.state.remote-handlers]
          [braid.core.client.core.events]
          [braid.core.client.core.subs]
          [braid.core.client.pages]
          [braid.core.client.ui.views.main]
          [braid.core.client.ui.views.styles]
          [braid.core.client.ui.views.header]
          [braid.core.client.ui.views.user-header]
          [braid.core.client.ui.views.autocomplete]
          [braid.core.client.group-admin.views.group-settings-page]]
         :clj
         [[braid.core.server.conf]
          [braid.core.server.routes.client]
          [braid.core.server.db.user]
          [braid.core.server.routes.api.modules]
          [braid.core.server.jobs]
          [braid.core.server.schema]
          [braid.core.server.sync]
          [braid.core.server.sync-handler]])))

; Currently, this module is non-idiomatic, in that it primarily references functions defined elsewhere
; As core gets split into seperate modules, the functions will be defined directly in respective core.clj namespace

#?(:cljs
   (do
     (defn register-root-view!
       "Add a new view to the app (when user is logged in).
        Will be put under body > #app > .app > .main >"
       [view]
       {:pre [(fn? view)]}
       (swap! braid.core.client.ui.views.main/root-views conj view))

     (defn register-group-header-button!
       "Add a new button to appear in the group header
       Expects a map with the following keys
        :title        string, for hover info
        :priority     number, for ordering
        :route-fn     secretary route fn that returns url
        :route-args   optional map of args to pass to route-fn
                      :group-id will be merged in"
       [config]
       {:pre [(util/valid? braid.core.client.ui.views.header/GroupHeaderItem config)]}
       (swap! braid.core.client.ui.views.header/group-header-buttons conj config))

     (defn register-user-header-menu-item!
       "Add a new link to appear in the user header
       Expects a map with the following keys
       :body         string, text to show
       :priority     number, for ordering
       :on-click     optional on-click function
       :route-fn     optional secretary route fn that returns url
       :route-args   optional map of args to pass to route-fn
                     :group-id will be merged in"
       [config]
       {:pre [(util/valid? braid.core.client.ui.views.user-header/UserHeaderItem config)]}
       (swap! braid.core.client.ui.views.user-header/user-header-menu-items conj config))

     (defn register-thread-header-item!
       "Adds a new view to a thread's header.
        Expects a map with the following keys:
        :priority   number, of ordering
        :view       reagent view fn (given thread map as first argument)"
       [config]
       {:pre [(util/valid? braid.core.client.ui.views.thread/header-item-dataspec config)]}
       (swap! braid.core.client.ui.views.thread/thread-header-items conj config))

     (defn register-thread-control!
       "Adds a new view to a thread's controls.
        Expects a map with the following keys:
        :priority   number, of ordering
        :view       reagent view fn (given thread map as first argument)"
       [config]
       {:pre [(util/valid? braid.core.client.ui.views.thread/thread-control-dataspec config)]}
       (swap! braid.core.client.ui.views.thread/thread-controls conj config))

     (defn register-system-page!
       "Registers a system page with its own URL.

       Expects a map with the following keys:
         :key      keyword
         :on-load  optional function to call
                   when page is navigated to
         :view   reagent view fn

       Link for page can be generated using:
        (braid.core.client.routes/system-page-path
             {:page-id __})"
       [page]
       {:pre [(util/valid? braid.core.client.pages/page-dataspec page)]}
       (swap! braid.core.client.pages/pages assoc (page :key) page))

     (defn register-group-page!
       "Registers a group page with its own URL.

       Expects a map with the following keys:
         :key      keyword
         :on-load  optional function to call
                   when page is navigated to
         :view   reagent view fn

       Link for page can be generated using:
        (braid.core.client.routes/page-path
             {:group-id __
              :page-id __))"
       [page]
       {:pre [(util/valid? braid.core.client.pages/page-dataspec page)]}
       (swap! braid.core.client.pages/pages assoc (page :key) page))

     (defn register-events!
       "Registers multiple re-frame event handlers, as if passed to reg-event-fx.

        Expects a map of event-keys to event-handler-fns."
       [event-map]
       {:pre [(map? event-map)
              (every? keyword? (keys event-map))
              (every? fn? (vals event-map))]}
       (braid.core.client.core.events/register-events! event-map))

     (defn register-subs!
       "Registers multiple re-frame subscription handlers, as if passed to reg-sub.

        Expects a map of sub-keys to sub-handler-fns."
       [sub-map]
       {:pre [(map? sub-map)
              (every? keyword? (keys sub-map))
              (every? fn? (vals sub-map))]}
       (braid.core.client.core.subs/register-subs! sub-map))

     (defn register-header-view!
       "Add a new view to appear in the header row at the top"
       [view]
       {:pre [(fn? view)]}
       (swap! braid.core.client.ui.views.header/header-views conj view))

     (defn register-event-listener!
       "Register a function to intercept re-frame events."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.client.core.events/event-listeners conj f))

     (defn register-autocomplete-engine!
       "Provide a sequence of functions that will act as autocomplete Handlers. The functions will recieve the current typed text as an argument and should return a sequence of autocomplete values."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.client.ui.views.new-message/autocomplete-engines conj f))

     (defn register-message-transform!
       "Register new stateless transformers to message bodies. The functions should expect to receive either some text or hiccup nodes and should return the same."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.client.ui.views.message/stateless-formatters conj f))

     (defn register-message-formatter!
       "Register new post-transform message formatters. The function will recieve the hiccup representing the message and should return the same."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.client.ui.views.message/post-transformers conj f))

     (defn register-footer-view!
       "Register a view to display after a message.
        View will receive message object as a parameter."
       [view]
       {:pre [(fn? view)]}
       (swap! braid.core.client.ui.views.message/footer-views conj view))

     (defn register-styles!
       "Add Garden CSS styles to the page styles"
       [styles]
       {:pre [(util/valid? braid.core.client.ui.views.styles/style-dataspec styles)]}
       (swap! braid.core.client.ui.views.styles/module-styles conj styles))

     (defn register-state!
       "Add a key and initial value to the default app state, plus an associated spec."
       [state spec]
       {:pre [(map? state)
              (map? spec)]}
       (braid.core.client.state/register-state! state spec))

     (defn register-incoming-socket-message-handlers!
       "Registers multiple server-side socket message handlers.

       Expects map of event-keys and handler-fns.
       Handler fn will be called with user-id and data arguments"
       [handler-map]
       {:pre [(map? handler-map)
              (every? keyword? (keys handler-map))
              (every? fn? (vals handler-map))]}
       (swap! braid.core.client.state.remote-handlers/incoming-socket-message-handlers merge handler-map))

     (defn register-initial-user-data-handler!
       "Add a handler that will run with the initial db & user-info recieved from the server. See `:register-initial-user-data` under `:clj`"
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.client.core.events/initial-user-data-handlers conj f))

     (defn register-group-setting!
       "Register a view to add to the group settings page. The view will recieve the group map as an argument."
       [view]
       {:pre [(fn? view)]}
       (swap! braid.core.client.group-admin.views.group-settings-page/extra-group-settings conj view)))

   :clj
   (do
     (defn register-config-var!
       "Add a keyword to be read from `env` and added to the `config` state"
       [var]
       {:pre [(keyword? var)]}
       (swap! braid.core.server.conf/config-vars conj var))

     (defn register-additional-script!
       "Add a javascript script tag to client html. Values can be a map with a `:src` or `:body` key or a function with no arguments, returing the same."
       [tag]
       {:pre [(util/valid? braid.core.server.routes.client/additional-script-dataspec tag)]}
       (swap! braid.core.server.routes.client/additional-scripts conj tag))

     (defn register-post-create-user-txn!
       "Add a function that will return a sequence of datomic txns to be called when a new user is created. The function will recieve the datomic id of the new user as an argument."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.server.db.user/post-create-txns conj f))

     (defn register-db-schema!
       "Add new datoms to the db schema"
       [schema]
       {:pre [(vector? schema)
              (every? (partial util/valid? braid.core.server.schema/rule-dataspec) schema)]}
       (swap! braid.core.server.schema/schema concat schema))

     (defn register-initial-user-data!
       "Add a map of key -> fn for getting the initial user data to be sent to the client. `fn` will recieve the user-id as its argument. See `:register-initial-user-data-handler` under `:cljs`"
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.server.sync/initial-user-data conj f))

     (defn register-server-message-handlers!
       "Add a map of websocket-event-name -> event-handler-fn to handle events from the client"
       [handler-defs]
       {:pre [(map? handler-defs)
              (every? keyword? (keys handler-defs))
              (every? fn? (vals handler-defs))]}
       (swap! braid.core.server.sync-handler/message-handlers merge handler-defs))

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
       {:pre [(util/valid? braid.core.server.routes.api.modules/route? route)]}
       (swap! braid.core.server.routes.api.modules/module-public-http-routes conj route))

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
       {:pre [(util/valid? braid.core.server.routes.api.modules/route? route)]}
       (swap! braid.core.server.routes.api.modules/module-private-http-routes conj route))

     (defn register-daily-job!
       "Add a recurring job that will run once a day. Expects a zero-arity function."
       [job-fn]
       {:pre [(fn? job-fn)]}
       (braid.core.server.jobs/register-daily-job! job-fn))))
