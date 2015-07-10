(ns chat.client.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-utils.core :refer [edn-xhr]]))

(enable-console-print!)

(def app-state (atom {:messages []}))

(defn guid []
  (rand-int 100000))

(defn transact! [ks f]
  (swap! app-state update-in ks f))

(defn seed! []
  (reset! app-state
          {:session {:user-id 1}
           :users {1 {:id 1
                      :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-08/4801271456_41230ac0b881cdf85c28_72.jpg"
                      :hidden-thread-ids []}
                   2 {:id 2
                      :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-09/4805955000_07dc272f0a7f9de7719e_192.jpg"
                      :hidden-thread-ids []}}
           :messages {99 {:id 99 :content "Hello?" :thread-id 123 :user-id 1 :created-at (js/Date. 1)}
                      98 {:id 98 :content "Hi!" :thread-id 123 :user-id 2 :created-at (js/Date. 2)}
                      97 {:id 97 :content "Oh, great, someone else is here." :thread-id 123 :user-id 1 :created-at (js/Date. 3)}
                      96 {:id 96 :content "Yep" :thread-id 123 :user-id 2 :created-at (js/Date. 4)}}}))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (transact! [:messages] #(let [id (guid)]
                            (assoc % id {:id id
                                         :content (data :content)
                                         :thread-id (or (data :thread-id) (guid))
                                         :user-id (get-in @app-state [:session :user-id])
                                         :created-at (js/Date.)}))))

(defmethod dispatch! :hide-thread [_ data]
  (transact! [:users (get-in @app-state [:session :user-id]) :hidden-thread-ids] #(conj % (data :thread-id))))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message"}
        (dom/img #js {:className "avatar" :src (get-in @app-state [:users (message :user-id) :icon])})
        (dom/div #js {:className "content"}
          (message :content))))))

(defn new-message-view [config owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message new"}
        (dom/textarea #js {:placeholder (config :placeholder)
                           :onKeyDown (fn [e]
                                        (when (and (= 13 e.keyCode) (= e.shiftKey false))
                                          (dispatch! :new-message {:thread-id (config :thread-id)
                                                                   :content (.. e -target -value)})
                                          (.preventDefault e)
                                          (aset (.. e -target) "value" "")))})))))
(defn thread-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (dom/div #js {:className "close"
                      :onClick (fn [_]
                                 (dispatch! :hide-thread {:thread-id (thread :id)}))} "×")
        (apply dom/div #js {:className "messages"}
          (om/build-all message-view (thread :messages)))
        (om/build new-message-view {:thread-id (thread :id) :placeholder "Reply..."})))))

(defn new-thread-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (om/build new-message-view {:placeholder "Start a new conversation..."})))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [remove-keys (fn [ks coll]
                          (apply dissoc coll ks))
            threads (->> (data :messages)
                         vals
                         (sort-by :created-at)
                         (group-by :thread-id)
                         (remove-keys (get-in data [:users (get-in data [:session :user-id]) :hidden-thread-ids]))
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
