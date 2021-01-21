(ns braid.chat.core
  "The majority of the core 'chat' functionality of Braid"
  (:require
   [braid.base.api :as base]
   [braid.chat.api :as chat]
   #?@(:clj
       [[braid.chat.schema :as schema]
        [braid.chat.seed :as seed]
        [braid.chat.server.initial-data :as initial-data]
        [braid.chat.socket-message-handlers :refer [socket-message-handlers]]]
       :cljs
       [[re-frame.core :refer [dispatch]]
        [braid.chat.client.remote-handlers]
        [braid.core.client.ui.views.autocomplete]
        [braid.core.client.store]
        [braid.popovers.api :as popovers]
        [braid.core.client.schema :as schema]
        [braid.core.client.state.helpers :as helpers :refer [key-by-id]]
        [braid.core.client.ui.styles.hover-menu]
        [braid.core.client.ui.styles.hover-cards]
        [braid.core.client.ui.styles.thread]
        [braid.core.client.ui.views.pages.global-settings :refer [global-settings-page-view]]
        [braid.core.client.invites.views.invite-page :refer [invite-page-view]]
        [braid.core.client.ui.views.pages.changelog :refer [changelog-view]]
        [braid.core.client.ui.views.pages.me :refer [me-page-view]]
        [braid.core.client.group-admin.views.group-settings-page :refer [group-settings-page-view]]])))

(defn init! []
  #?(:clj
     (do
       (base/register-db-schema! schema/schema)
       (base/register-db-seed-txns! seed/txns)
       ;; TODO some of these might better belong elsewhere
       (doseq [k [:aws-bucket
                  :aws-region
                  :aws/credentials-provider
                  :cookie-secure
                  :db-url
                  :github-client-id
                  :github-client-secret
                  :hmac-secret
                  :http-only
                  :mailgun-domain
                  :mailgun-password
                  :site-url]]
         (base/register-config-var! k))

       (base/register-initial-user-data! initial-data/initial-data-for-user)

       (base/register-server-message-handlers!
         socket-message-handlers))

     :cljs
     (do
       (braid.chat.client.remote-handlers/init!)
       (chat/register-autocomplete-engine!
        braid.core.client.ui.views.autocomplete/user-autocomplete-engine)

       (chat/register-autocomplete-engine!
        braid.core.client.ui.views.autocomplete/tag-autocomplete-engine)

       (base/register-state! braid.core.client.store/initial-state
                             braid.core.client.store/AppState)

       (popovers/register-popover-styles!
        braid.core.client.ui.styles.hover-cards/>user-card)

       (popovers/register-popover-styles!
        braid.core.client.ui.styles.hover-cards/>tag-card)

       (popovers/register-popover-styles!
        (braid.core.client.ui.styles.hover-menu/>hover-menu))

       (popovers/register-popover-styles!
        braid.core.client.ui.styles.thread/add-tag-popover-styles)

       (base/register-system-page!
        {:key :global-settings
         :view global-settings-page-view})

       (base/register-initial-user-data-handler!
        (fn [db data]
          (-> db
              (assoc :session {:user-id (data :user-id)})
              (assoc-in [:user :subscribed-tag-ids]
                        (set (data :user-subscribed-tag-ids)))
              (assoc :groups (key-by-id (data :user-groups)))
              (assoc :invitations (data :invitations))
              (assoc :threads (key-by-id (data :user-threads)))
              (assoc :group-threads
                     (into {}
                           (map (fn [[g t]] [g (into #{} (map :id) t)]))
                           (group-by :group-id (data :user-threads))))
              (assoc-in [:user :open-thread-ids]
                        (set (map :id (data :user-threads))))
              (assoc :temp-threads (->> (data :user-groups)
                                        (map :id)
                                        (reduce (fn [memo group-id]
                                                  (->> (schema/make-temp-thread group-id)
                                                       (assoc memo group-id)))
                                                {})))
              (helpers/add-tags (data :tags))
              (helpers/set-preferences (data :user-preferences)))))

       (chat/register-group-page!
        {:key :settings
         :view group-settings-page-view})

       (chat/register-group-page!
        {:key :me
         :view me-page-view})

       (chat/register-group-page!
        {:key :invite
         :view invite-page-view})

       (base/register-system-page!
        {:key :changelog
         :view changelog-view}))))
