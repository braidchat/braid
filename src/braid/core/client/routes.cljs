(ns braid.core.client.routes
  (:require
   [braid.core.client.router :as router]
   [braid.core.client.pages :as pages]
   [re-frame.core :refer [dispatch subscribe]]
   [secretary.core :as secretary :refer-macros [defroute]]))

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

(defroute group-settings-path "/groups/:group-id/settings" [group-id]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :settings}]]))

(defroute search-page-path "/groups/:group-id/search/:query" [group-id query]
  (dispatch [:set-group-and-page [(uuid group-id) {:type :search
                                                   :search-query query}]])
  (dispatch [:set-page-loading true])
  (dispatch [:search-history [query (uuid group-id)]]))

(defroute join-group-path "/groups/:group-id/join" [group-id]
  (dispatch [:braid.core.client.gateway.events/initialize :join-group])
  (dispatch [:set-group-and-page [nil {:type :login}]]))

(defroute page-path "/groups/:group-id/:page-id" [group-id page-id query-params]
  (let [page (merge {:type (keyword page-id)
                     :page-id (keyword page-id)
                     :group-id (uuid group-id)}
                    query-params)]
    (dispatch [:set-group-and-page [(uuid group-id) page]])
    (pages/on-load! (keyword page-id) page)))

(defroute other-path "/pages/:page-id" [page-id]
  (case page-id
    "group-explore" (dispatch [:core/load-public-groups])
    nil)
  (dispatch [:set-group-and-page [nil {:type (keyword page-id)}]]))

(defroute index-path "/" {}
  (dispatch [:set-group-and-page [nil {:type :login}]])
  (dispatch [:redirect-from-root]))
