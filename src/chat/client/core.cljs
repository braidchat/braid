(ns chat.client.core
  (:require [om.core :as om]
            [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.views :as views]
            [chat.client.clj-highlighter :as highlighter]
            [chat.client.dispatcher :as dispatcher]
            [chat.client.router :as router]))

(enable-console-print!)

(defn ^:export init []
  (.addEventListener js/document "visibilitychange"
                (fn [e] (store/set-window-visibility! (= "visible" (.-visibilityState js/document)))))
  (sync/make-socket!)
  (sync/start-router!))

(highlighter/install-highlighter)
(om/root views/app-view store/app-state
           {:target (. js/document (getElementById "app"))})

(router/init)
