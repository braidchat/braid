(ns chat.client.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-utils.core :refer [edn-xhr]]))

(enable-console-print!)

(def app-state (atom {:messages []}))

(defn guid []
  (rand 100000))

(defn transact! [ks f]
  (swap! app-state update-in ks f))

(defn seed! []
  (transact! [:messages] (constantly [{:content "Hello?" :thread-id 123 :user-id 1}
                                     {:content "Hi!" :thread-id 123 :user-id 2}
                                     {:content "Oh, great, someone else is here." :thread-id 123 :user-id 1}
                                     {:content "Yep" :thread-id 123 :user-id 2}])))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (transact! [:messages] #(conj % {:content (data :content)
                                   :thread-id (or (data :thread-id) (guid))})))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message"}
        (dom/div #js {:className "content"}
          (message :content))))))

(defn new-reply-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message new"}
        (dom/textarea #js {:placeholder "Reply..."
                           :onKeyDown (fn [e]
                                        (when (and (= 13 e.keyCode) (= e.shiftKey false))
                                          (dispatch! :new-message {:thread-id (thread :id)
                                                                   :content (.. e -target -value)})
                                          (.preventDefault e)
                                          (aset (.. e -target) "value" "")))})))))
(defn thread-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (apply dom/div #js {:className "messages"}
          (om/build-all message-view (thread :messages)))
        (om/build new-reply-view thread)))))

(defn new-thread-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (dom/div #js {:className "message new"}
          (dom/textarea #js {:placeholder "Start a new conversation..."
                           :onKeyDown (fn [e]
                                        (when (and (= 13 e.keyCode) (= e.shiftKey false))
                                          (dispatch! :new-message {:content (.. e -target -value)})
                                          (.preventDefault e)
                                          (aset (.. e -target) "value" "")))}))))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [threads (->> (data :messages)
                         (group-by :thread-id)
                         (map (fn [[id ms]] {:id id
                                             :messages ms})))]
        (dom/div nil
          (apply dom/div nil
            (concat (om/build-all thread-view threads)
                    [(om/build new-thread-view {})])))))))


(defn init []
  (om/root app-view app-state
           {:target (. js/document (getElementById "app"))})
  (seed!))
