(ns braid.core.core
  "Braid core"
  (:require
    #?@(:cljs
         [[braid.core.client.ui.views.new-message]
          [braid.core.client.ui.views.message]
          [braid.core.client.state]
          [braid.core.client.core.events]
          [braid.core.client.ui.views.styles]
          [braid.core.client.core.events]
          [braid.core.client.ui.views.header]
          [braid.core.client.ui.views.autocomplete]
          [braid.core.client.group-admin.views.group-settings-page]]
         :clj
         [[braid.core.server.conf]
          [braid.core.server.routes.client]
          [braid.core.server.db.user]
          [braid.core.server.schema]
          [braid.core.server.sync]
          [braid.core.server.sync-handler]])))

; Currently, this module is non-idiomatic, in that it primarily references functions defined elsewhere
; As core gets split into seperate modules, the functions will be defined directly in respective core.clj namespace

#?(:cljs
   (do
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

     (defn register-styles!
       "Add Garden CSS styles to the page styles"
       [styles]
       {:pre [(vector? styles)]}
       (swap! braid.core.client.ui.views.styles/module-styles conj styles))

     (defn register-state!
       "Add a key and initial value to the default app state, plus an associated spec."
       [state spec]
       {:pre [(map? state)
              (map? spec)]}
       (braid.core.client.state/register-state! state spec))

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
       [vars]
       {:pre [(vector? vars)
              (every? keyword? vars)]}
       (swap! braid.core.server.conf/config-vars concat vars))

     (defn register-additional-script!
       "Add a javascript script tag to client html. Values can be a map with a `:src` or `:body` key or a function with no arguments, returing the same."
       [tag]
       {:pre [(or (fn? tag)
                  (and (map? tag)
                    (or (:src tag) (:body tag))))]}
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
              (every? map? schema)]}
       (swap! braid.core.server.schema/schema concat schema))

     (defn register-initial-user-data!
       "Add a map of key -> fn for getting the initial user data to be sent to the client. `fn` will recieve the user-id as its argument. See `:register-initial-user-data-handler` under `:cljs`"
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.server.sync/initial-user-data conj f))

     (defn register-server-message-handler!
       "Add a map of websocket-event-name -> event-handler-fn to handle events from the client"
       [handler-defs]
       {:pre [(map? handler-defs)
              (every? keyword? (keys handler-defs))
              (every? fn? (vals handler-defs))]}
       (swap! braid.core.server.sync-handler/message-handlers merge handler-defs))))

(defn init! []
  #?(:cljs
     (do
       (register-autocomplete-engine!
         braid.core.client.ui.views.autocomplete/bot-autocomplete-engine)

       (register-autocomplete-engine!
         braid.core.client.ui.views.autocomplete/user-autocomplete-engine)

       (register-autocomplete-engine!
         braid.core.client.ui.views.autocomplete/tag-autocomplete-engine)

       (register-state! braid.core.client.store/initial-state
                        braid.core.client.store/AppState))))

