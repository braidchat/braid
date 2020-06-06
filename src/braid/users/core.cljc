(ns braid.users.core
  (:require
   [braid.core.api :as core]
   #?@(:cljs
       [[braid.users.client.views.users-page :as views]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-group-page!
        {:key :users
         :view views/users-page-view}))))
