(ns braid.users.core
  (:require
   [braid.core.api :as core]
   #?@(:cljs
       [[braid.users.client.views.users-page :as views]
        [braid.core.client.routes :as routes]])))



(defn init! []
  #?(:cljs
     (do
       (core/register-group-page!
        {:key :users
         :view views/users-page-view})
       (core/register-admin-header-item!
        {:class "users"
         :route-fn routes/group-page-path
         :route-args {:page-id "users"}
         :body "Users"}))))
