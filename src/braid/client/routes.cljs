(ns braid.client.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [braid.client.store :as store]
            [braid.client.state :as state]
            [braid.client.router :as router])
  (:import [goog.history Html5History]
           [goog Uri]))

(defn go-to! [path]
  (router/go-to path))

(defroute page-path "/:group-id/:page-id" [group-id page-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type (keyword page-id)}))

(defroute inbox-page-path "/:group-id/inbox" [group-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :inbox}))

(defroute recent-page-path "/:group-id/recent" [group-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :recent}))

(defroute invite-page-path "/:group-id/invite" [group-id ]
  (store/set-group-and-page! (UUID. group-id nil) {:type :invite}))

(defroute users-page-path "/:group-id/users" [group-id ]
  (store/set-group-and-page! (UUID. group-id nil) {:type :users}))

(defroute user-page-path "/:group-id/user/:user-id" [group-id user-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :user
                                                   :id (UUID. user-id nil)}))

(defroute tag-page-path "/:group-id/tag/:tag-id" [group-id tag-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :tag
                                                   :id (UUID. tag-id nil)}))

(defroute bots-path "/:group-id/bots" [group-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :bots}))

(defroute uploads-path "/:group-id/uploads" [group-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :uploads}))

(defroute thread-path "/:group-id/thread/:thread-id" [group-id thread-id]
  (store/set-group-and-page! (UUID. group-id nil)
                             {:type :thread
                              :thread-ids [(UUID. thread-id nil)]}))

(defroute group-settings-path "/:group-id/settings" [group-id]
  (store/set-group-and-page! (UUID. group-id nil) {:type :settings}))

(defroute search-page-path "/:group-id/search/:query" [group-id query]
  (store/set-group-and-page! (UUID. group-id nil) {:type :search
                                                   :search-query query}))

(defroute other-path "/:page-id" [page-id]
  (store/set-group-and-page! nil {:type (keyword page-id)}))

(defroute index-path "/" {}
  (when-let [group-id (->> @store/app-state
                           ((juxt (comp vals :groups)
                                  (comp :groups-order :preferences)))
                           (apply state/order-groups)
                           first
                           :id)]
    (go-to! (inbox-page-path {:group-id group-id}))))

(defn current-group []
  (store/open-group-id))

(defn current-path? [path]
  (= path (.getPath (.parse Uri js/window.location))))
