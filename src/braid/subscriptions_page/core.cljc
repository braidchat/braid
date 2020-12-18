(ns braid.subscriptions-page.core
  "Page for user to manage tag subscriptions"
  (:require
   [braid.base.api :as base]
   [braid.chat.api :as chat]
   #?@(:cljs
       [[braid.core.client.routes :as routes]
        [braid.subscriptions-page.styles :as styles]
        [braid.subscriptions-page.ui :as ui]])))

(defn init! []
  #?(:cljs
     (do
       (chat/register-group-page!
        {:key :tags
         :view ui/tags-page-view})

       (chat/register-user-header-menu-item!
        {:body "Manage Subscriptions"
         :route-fn routes/group-page-path
         :route-args {:page-id "tags"}
         :icon \uf02c
         :priority 10})

       (base/register-styles!
        [:.app>.main
         styles/tags-page]))))
