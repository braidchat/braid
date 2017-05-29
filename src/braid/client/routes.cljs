(ns braid.client.routes
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [secretary.core :as secretary :refer-macros [defroute]]
    [braid.client.router :as router]))

(defn go-to! [path]
  (router/go-to path))

(defroute inbox-page-path "/groups/:group-id" [group-id]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :inbox}]]))

(defroute recent-page-path "/groups/:group-id/recent" [group-id]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :recent}]])
  (dispatch [:set-page-loading true])
  (dispatch [:load-recent-threads
             {:group-id (uuid group-id)
              :on-complete (fn [_]
                             (dispatch [:set-page-loading false]))
              :on-error (fn [e]
                          (dispatch [:set-page-loading false])
                          (dispatch [:set-page-error true]))}]))

(defroute invite-page-path "/groups/:group-id/invite" [group-id ]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :invite}]]))

(defroute bots-path "/groups/:group-id/bots" [group-id]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :bots}]]))

(defroute uploads-path "/groups/:group-id/uploads" [group-id]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :uploads}]]))

(defroute thread-path "/groups/:group-id/thread/:thread-id" [group-id thread-id]
  (dispatch [:set-group-and-page [(uuid group-id)
                                  {:type :thread
                                   :thread-ids [(uuid thread-id)]}]]))

(defroute group-settings-path "/groups/:group-id/settings" [group-id]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :settings}]]))

(defroute search-page-path "/groups/:group-id/search/:query" [group-id query]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :search
                                                   :search-query query}]])
  (dispatch [:set-page-loading true])
  (dispatch [:search-history [query (uuid group-id)]]))

(defroute page-path "/groups/:group-id/:page-id" [group-id page-id]
  (dispatch [:set-group-and-page [(uuid group-id) {:type (keyword page-id)}]]))

(defroute other-path "/pages/:page-id" [page-id]
  (dispatch [:set-group-and-page [nil {:type (keyword page-id)}]]))

(defroute index-path "/" {}
  (dispatch [:redirect-from-root]))
