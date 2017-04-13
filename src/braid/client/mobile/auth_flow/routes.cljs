(ns braid.client.mobile.auth-flow.routes
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [secretary.core :as secretary :refer-macros [defroute]]
    [braid.client.router :as router]))

(defroute welcome-page-path "/welcome" []
  (dispatch [:set-auth-flow nil nil]))

(defroute auth-flow-path "/welcome/:method/:stage" [method stage]
  (dispatch [:set-auth-flow (keyword method) (keyword stage)]))
