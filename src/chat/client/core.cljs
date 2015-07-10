(ns chat.client.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-utils.core :refer [edn-xhr]]))

(enable-console-print!)

(def app-state (atom {}))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        "hello world"
        ))))


(defn init []
  (om/root app-view app-state
           {:target (. js/document (getElementById "app"))}))
