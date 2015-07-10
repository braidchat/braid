(ns chat.client.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-utils.core :refer [edn-xhr]]))

(enable-console-print!)

(def app-state (atom {:messages []}))

(defn transact! [ks f]
  (swap! app-state update-in ks f))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (transact! [:messages] #(conj % {:content data})))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message"}
        (dom/div #js {:className "content"}
          (message :content))))))

(defn new-message-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message new"}
        (dom/textarea #js {:placeholder "Start a new conversation..."
                           :onKeyDown (fn [e]
                                        (when (and (= 13 e.keyCode) (= e.shiftKey false))
                                          (dispatch! :new-message (.. e -target -value))
                                          (.preventDefault e)
                                          (aset (.. e -target) "value" "")))})))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (apply dom/div nil
          (om/build-all message-view (data :messages)))
        (om/build new-message-view {})))))


(defn init []
  (om/root app-view app-state
           {:target (. js/document (getElementById "app"))}))
