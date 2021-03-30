(ns braid.users.core
  (:require
   [braid.base.api :as base]
   [braid.chat.api :as chat]
   #?@(:cljs
       [[braid.users.client.views.users-page :as views]
        [braid.users.client.views.users-page-styles :as styles]
        [braid.core.client.routes :as routes]])))

(defn init! []
  #?(:cljs
     (do
       (chat/register-group-page!
        {:key :users
         :view views/users-page-view})
       (chat/register-admin-header-item!
        {:class "users"
         :route-fn routes/group-page-path
         :route-args {:page-id "users"}
         :icon \uf0c0
         :body "Users"})
       (base/register-styles! styles/users-page))))
