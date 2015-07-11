(ns chat.client.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-utils.core :refer [edn-xhr]]
            [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.views :as views]
            [chat.client.dispatcher :as dispatcher]))

(enable-console-print!)

(defn init []
  (om/root views/app-view store/app-state
           {:target (. js/document (getElementById "app"))})
  (dispatcher/dispatch! :seed nil)
  (sync/start-router!))
