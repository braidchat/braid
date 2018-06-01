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
       [& args]
       (apply braid.core.client.ui.views.header/register-header-view! args))

     (defn register-event-listener!
       "Register a function to intercept re-frame events."
       [& args]
       (apply braid.core.client.core.events/register-event-listener! args))

     (defn register-autocomplete-engine!
       "Provide a sequence of functions that will act as autocomplete Handlers. The functions will recieve the current typed text as an argument and should return a sequence of autocomplete values."
       [& args]
       (apply braid.core.client.ui.views.new-message/register-autocomplete-engines! args))

     (defn register-message-transform!
       "Register new stateless transformers to message bodies. The functions should expect to recieve either some text or hiccup nodes and should return the same."
       [& args]
       (apply braid.core.client.ui.views.message/register-stateless-formatters! args))

     (defn register-message-formatter!
       "Register new post-transform message formatters. The function will recieve the hiccup representing the message and should return the same."
       [& args]
       (apply braid.core.client.ui.views.message/register-post-transformers! args))

     (defn register-styles!
       "Add Garden CSS styles to the page styles"
       [& args]
       (apply braid.core.client.ui.views.styles/register-styles! args))

     (defn register-state!
       "Add a key and initial value to the default app state, plus an associated spec."
       [& args]
       (apply braid.core.client.state/register-state! args))

     (defn register-initial-user-data-handler!
       "Add a handler that will run with the initial db & user-info recieved from the server. See `:register-initial-user-data` under `:clj`"
       [& args]
       (apply braid.core.client.core.events/register-initial-user-data-handler! args))

     (defn register-group-setting!
       "Register a view to add to the group settings page. The view will recieve the group map as an argument."
       [& args]
       (apply braid.core.client.group-admin.views.group-settings-page/register-group-setting! args)))

   :clj
   (do
     (defn register-config-var!
       "Add a keyword to be read from `env` and added to the `config` state"
       [& args]
       (apply braid.core.server.conf/register-config-var! args))

     (defn register-additional-script!
       "Add a javascript script tag to client html. Values can be a map with a `:src` or `:body` key or a function with no arguments, returing the same."
       [& args]
       (apply braid.core.server.routes.client/register-additional-script! args))

     (defn register-post-create-user-txn!
       "Add a function that will return a sequence of datomic txns to be called when a new user is created. The function will recieve the datomic id of the new user as an argument."
       [& args]
       (apply braid.core.server.db.user/register-post-create-user-txn! args))

     (defn register-db-schema!
       "Add new datoms to the db schema"
       [& args]
       (apply braid.core.server.schema/register-db-schema! args))

     (defn register-initial-user-data!
       "Add a map of key -> fn for getting the initial user data to be sent to the client. `fn` will recieve the user-id as its argument. See `:register-initial-user-data-handler` under `:cljs`"
       [& args]
       (apply braid.core.server.sync/register-initial-user-data! args))

     (defn register-server-message-handler!
       "Add a map of websocket-event-name -> event-handler-fn to handle events from the client"
       [& args]
       (apply braid.core.server.sync-handler/register-server-message-handlers! args))))

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

