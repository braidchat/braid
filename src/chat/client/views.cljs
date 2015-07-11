(ns chat.client.views
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message"}
        (dom/img #js {:className "avatar" :src (get-in @store/app-state [:users (message :user-id) :icon])})
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
