(ns braid.chat.api
  (:require
    [braid.core.common.util :as util]
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.chat.client.events]
          [braid.chat.client.subs]
          [braid.core.client.ui.views.new-message]
          [braid.core.client.ui.views.message]
          [braid.core.client.ui.views.header]
          [braid.core.client.ui.views.thread-header]
          [braid.core.client.ui.views.user-header]
          [braid.core.client.ui.views.autocomplete]
          [braid.core.client.ui.views.new-message-action-button]
          [braid.core.client.group-admin.views.group-settings-page]]
         :clj
         [[braid.chat.db.user]
          [braid.chat.socket-message-handlers]
          [braid.core.server.sync-handler]
          [braid.core.server.sync-helpers]])))

#?(:cljs
   (do

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
       {:pre [(util/valid? braid.core.client.ui.views.thread-header/header-item-dataspec config)]}
       (swap! braid.core.client.ui.views.thread-header/thread-header-items conj config))

     (defn register-thread-control!
       "Adds a new view to a thread's controls.
        Expects a map with the following keys:
        :priority   number, of ordering
        :view       reagent view fn (given thread map as first argument)"
       [config]
       {:pre [(util/valid? braid.core.client.ui.views.thread-header/thread-control-dataspec config)]}
       (swap! braid.core.client.ui.views.thread-header/thread-controls conj config))

     (defn register-group-page!
       "Registers a group page with its own URL.

       Expects a map with the following keys:
         :key      keyword
         :on-load  (optional) function to call
                   when page is navigated to
         :on-exit  (optional) function to call
                   when page is navigated away from
         :view   reagent view fn
         :styles  (optional) garden styles for the page

       Link for page can be generated using:
        (braid.core.client.routes/group-page-path
             {:group-id __
              :page-id __))"
       [page]
       ;; at the moment, is the same
       (base/register-system-page! page))

     (defn register-header-view!
       "Add a new view to appear in the header row at the top"
       [view]
       {:pre [(fn? view)]}
       (swap! braid.core.client.ui.views.header/header-views conj view))

     (defn register-admin-header-item!
       "Add a new entry to the admin menu"
       [item]
       (swap! braid.core.client.ui.views.header/admin-header-items
              conj item))

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

     (defn register-group-setting!
       "Register a view to add to the group settings page. The view will recieve the group map as an argument."
       [view]
       {:pre [(fn? view)]}
       (swap! braid.core.client.group-admin.views.group-settings-page/extra-group-settings conj view))

     (defn register-new-message-action-menu-item!
       [menu-item]
       {:pre [(util/valid? braid.core.client.ui.views.header-item/HeaderItem menu-item)]}
       (swap! braid.core.client.ui.views.new-message-action-button/menu-items conj menu-item))

     (defn register-message-sender-view!
       "Sender-views is a function that will recieve the id of the
       sender and should return a map with the keys :avatar, :info,
       and :name, which will be reagent views."
       [sender-views]
       {:pre [(fn? sender-views)]}
       (swap! braid.core.client.ui.views.message/message-sender-views conj sender-views)))

   :clj
   (do
     (defn register-post-create-user-txn!
       "Add a function that will return a sequence of datomic txns to be called when a new user is created. The function will recieve the datomic id of the new user as an argument."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.chat.db.user/post-create-txns conj f))

     (defn register-anonymous-group-load!
       "Add function that will recieve the group id and information for the anonymous group being loaded and should return an updated map of data."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.core.server.sync-handler/anonymous-load-group conj f))

     (defn register-new-message-callback!
       "Register a function that will be called when a new message is created.
       The function will be passed a map containing the new message."
       [callback]
       {:pre [(fn? callback)]}
       (swap! braid.chat.socket-message-handlers/new-message-callbacks conj callback))

     (defn register-group-broadcast-hook!
       "Add a function to be called when broadcasting a group change.
       The function will receive the group-id and a map of the change
       information."
       [hook-fn]
       {:pre [(fn? hook-fn)]}
       (swap! braid.core.server.sync-helpers/group-change-broadcast-hooks conj hook-fn))))
