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

(defn tag-view [tag owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "tag"}
        (dom/label nil
          (dom/input #js {:type "checkbox"
                          :checked (tag :subscribed?)
                          :onClick (fn [_]
                                     (if (tag :subscribed?)
                                       (dispatch! :unsubscribe-from-tag (tag :id))
                                       (dispatch! :subscribe-to-tag (tag :id))))})
          (tag :name))))))

(defn tags-view [tags owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "tags"}
        (om/build-all tag-view tags)))))

(defn chat-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [remove-keys (fn [ks coll]
                          (apply dissoc coll ks))
            open-thread-ids (set (data :open-thread-ids))
            threads (->> (data :messages)
                         vals
                         (sort-by :created-at)
                         (group-by :thread-id)
                         (map (fn [[id ms]] {:id id
                                             :messages ms}))
                         (filter (fn [t] (contains? open-thread-ids (t :id)))))
            tags (->> (data :tags)
                      vals
                      (map (fn [tag]
                             (assoc tag :subscribed?
                               (contains? (get-in @store/app-state [:user :subscribed-tag-ids]) (tag :id))))))

            ]
        (dom/div nil
          (dom/div #js {:className "user-meta"}
            (dom/img #js {:className "avatar"
                          :src (let [user-id (get-in @store/app-state [:session :user-id])]
                                 (get-in @store/app-state [:users user-id :icon]))})
            (dom/div #js {:className "logout"
                          :onClick (fn [_] (dispatch! :logout nil))} "×"))
          (om/build tags-view tags)
          (apply dom/div nil
            (concat (om/build-all thread-view threads)
                    [(om/build new-thread-view {})])))))))

(defn login-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:email ""
       :password ""})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "login"}
        (dom/input
          #js {:placeholder "Email"
               :type "text"
               :value (state :email)
               :onChange (fn [e] (om/set-state! owner :email (.. e -target -value)))})
        (dom/input
          #js {:placeholder "Password"
               :type "password"
               :value (state :password)
               :onChange (fn [e] (om/set-state! owner :password (.. e -target -value)))})
        (dom/button
          #js {:onClick (fn [e]
                          (dispatch! :auth {:email (state :email)
                                            :password (state :password)}))}
          "Let's do this!")))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (if (data :session)
          (om/build chat-view data)
          (om/build login-view data))))))
