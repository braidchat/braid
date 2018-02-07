(ns braid.core.client.mobile.auth-flow.routes
  (:require
   [braid.core.client.router :as router]
   [re-frame.core :refer [dispatch subscribe]]
   [secretary.core :as secretary :refer-macros [defroute]]))

(defroute welcome-page-path "/welcome" []
  (dispatch [:set-auth-flow nil nil]))

(defroute auth-flow-path "/welcome/:method/:stage" [method stage]
  (dispatch [:set-auth-flow (keyword method) (keyword stage)]))
