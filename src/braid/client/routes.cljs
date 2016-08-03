(ns braid.client.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [braid.client.store :as store]
            [braid.client.state :as state]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.router :as router]))

(defn go-to! [path]
  (router/go-to path))

(defroute inbox-page-path "/:group-id/inbox" [group-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :inbox}]))

(defroute recent-page-path "/:group-id/recent" [group-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :recent}])
  (dispatch! :set-page-loading true)
  (dispatch! :load-recent-threads
             {:group-id (uuid group-id)
              :on-complete (fn [_]
                             (dispatch! :set-page-loading false))
              :on-error (fn [e]
                          (dispatch! :set-page-loading false)
                          (dispatch! :set-page-error true))}))

(defroute invite-page-path "/:group-id/invite" [group-id ]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :invite}]))

(defroute users-page-path "/:group-id/users" [group-id ]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :users}]))

(defroute user-page-path "/:group-id/user/:user-id" [group-id user-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :user
                                                   :id (uuid user-id)}]))

(defroute tag-page-path "/:group-id/tag/:tag-id" [group-id tag-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :tag
                                                   :id (uuid tag-id)}])
  (dispatch! :set-page-loading true)
  (dispatch! :threads-for-tag {:tag-id (uuid tag-id)
                               :on-complete
                               (fn [_]
                                 (dispatch! :set-page-loading false))}))

(defroute bots-path "/:group-id/bots" [group-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :bots}]))

(defroute uploads-path "/:group-id/uploads" [group-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :uploads}]))

(defroute thread-path "/:group-id/thread/:thread-id" [group-id thread-id]
  (dispatch! :set-group-and-page [(uuid group-id)
                                  {:type :thread
                                   :thread-ids [(uuid thread-id)]}]))

(defroute group-settings-path "/:group-id/settings" [group-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :settings}]))

(defroute search-page-path "/:group-id/search/:query" [group-id query]
  (dispatch! :set-group-and-page [(uuid group-id) {:type :search
                                                   :search-query query}])
  (dispatch! :set-page-loading true)
  (dispatch! :search-history [query (uuid group-id)]))

(defroute other-path "/:page-id" [page-id]
  (dispatch! :set-group-and-page [nil {:type (keyword page-id)}]))

(defroute page-path "/:group-id/:page-id" [group-id page-id]
  (dispatch! :set-group-and-page [(uuid group-id) {:type (keyword page-id)}]))

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
