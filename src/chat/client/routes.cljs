(ns chat.client.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [chat.client.store :as store]
            [chat.client.router :as router]))

(defn go-to! [path]
  (router/go-to path))

(defroute page-path "/:group-id/:page-id" [group-id page-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type (keyword page-id)}))

(defroute other-path "/:page-id" [page-id]
  (store/set-group-and-page! nil {:type (keyword page-id)}))

(defroute inbox-page-path "/:group-id/inbox" [group-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :inbox}))

(defroute user-page-path "/:group-id/user/:user-id" [group-id user-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :user
                                                   :id (UUID. user-id nil)}))

(defroute tag-page-path "/:group-id/tag/:tag-id" [group-id tag-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :channel
                                                   :id (UUID. tag-id nil)}))

(defroute search-page-path "/:group-id/search/:query" [group-id query]
  (store/set-group-and-page! (UUID. group-id nil) {:type :search
                                                   :search-query query}))

(defroute index-path "/" {}
  (when-let [group-id (-> (@store/app-state :groups) vals first :id)]
    (go-to! (inbox-page-path {:group-id group-id}))))

(defn current-group []
  (get-in @store/app-state [:open-group-id]))

