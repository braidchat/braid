(ns braid.core.client.routes
  (:require
   [braid.base.client.router :as router]
   [braid.base.client.pages :as pages]
   [re-frame.core :refer [dispatch subscribe]]
   [secretary.core :as secretary :refer-macros [defroute]]))

(defn go-to! [path]
  (router/go-to path))

(defroute join-group-path "/groups/:group-id/join" [group-id]
  (dispatch [:braid.core.client.gateway.events/initialize! :join-group])
  (dispatch [:set-group-and-page! [nil {:type :login}]]))

(defroute group-page-path "/groups/:group-id/:page-id" [group-id page-id query-params]
  (let [page (merge {:type (keyword page-id)
                     :page-id (keyword page-id)
                     :group-id (uuid group-id)}
                    query-params)]
    (pages/on-exit! (:page-id @(subscribe [:page])) @(subscribe [:page]))
    (dispatch [:set-group-and-page! [(uuid group-id) page]])
    (pages/on-load! (keyword page-id) page)))

;; invites end up here
(defroute group-path "/groups/:group-id" [group-id]
  (router/go-to (group-page-path {:group-id group-id
                                  :page-id "inbox"})))

(defroute system-page-path "/pages/:page-id" [page-id query-params]
  (let [page (merge {:type (keyword page-id)
                     :page-id (keyword page-id)}
                    query-params)]
    (pages/on-exit! (:page-id @(subscribe [:page])) @(subscribe [:page]))
    (dispatch [:set-group-and-page! [nil page]])
    (pages/on-load! (keyword page-id) page)))

(defroute index-path "/" {}
  (dispatch [:set-group-and-page! [nil {:type :login}]])
  (dispatch [:redirect-from-root!]))
