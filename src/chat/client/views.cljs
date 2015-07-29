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
        (dom/img #js {:className "avatar" :src (get-in @store/app-state [:users (message :user-id) :avatar])})
        (dom/div #js {:className "content"}
          (message :content))))))

(defn new-message-view [config owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message new"}
        (dom/textarea #js {:placeholder (config :placeholder)
                           :onKeyDown
                           (fn [e]
                             (when (and (= 13 e.keyCode) (= e.shiftKey false))
                               (let [text (.. e -target -value)]
                                 (dispatch! :new-message {:thread-id (config :thread-id)
                                                          :content text}))
                               (.preventDefault e)
                               (aset (.. e -target) "value" "")))})))))

(defn tag->color [tag]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> (tag :id)
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",60%,60%)")))

(defn thread-tags-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (let [tags (->> (thread :tag-ids)
                      (map #(get-in @store/app-state [:tags %])))]
        (apply dom/div #js {:className "tags"}
          (map (fn [tag]
                 (dom/div #js {:className "tag"
                               :style #js {:background-color (tag->color tag)}}
                   (tag :name))) tags))))))

(defn thread-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (dom/div #js {:className "close"
                      :onClick (fn [_]
                                 (dispatch! :hide-thread {:thread-id (thread :id)}))} "×")
        (om/build thread-tags-view thread)
        (apply dom/div #js {:className "messages"}
          (om/build-all message-view (->> (thread :messages)
                                          (sort-by :created-at))
                        {:key :id}))
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
      (dom/div #js {:className "tag"
                    :onClick (fn [_]
                               (if (tag :subscribed?)
                                 (dispatch! :unsubscribe-from-tag (tag :id))
                                 (dispatch! :subscribe-to-tag (tag :id))))}
        (dom/div #js {:className "color-block"
                      :style #js {:backgroundColor (tag->color tag)}}
          (when (tag :subscribed?)
            "✔"))
        (dom/span #js {:className "name"}
          (tag :name))))))

(defn tags-view [tags owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "tags"}
        (om/build-all tag-view tags)))))


(defn new-tag-view [_ owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:className "new-tag"
                      :onKeyDown
                      (fn [e]
                        (when (= 13 e.keyCode)
                          (let [text (.. e -target -value)]
                            (dispatch! :create-tag text))
                          (.preventDefault e)
                          (aset (.. e -target) "value" "")))
                      :placeholder "New Tag"}))))

(defn chat-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [threads (vals (data :threads))
            tags (->> (data :tags)
                      vals
                      (map (fn [tag]
                             (assoc tag :subscribed?
                               (contains? (get-in @store/app-state [:user :subscribed-tag-ids]) (tag :id))))))]
        (dom/div nil
          (dom/div #js {:className "meta"}
            (dom/img #js {:className "avatar"
                          :src (let [user-id (get-in @store/app-state [:session :user-id])]
                                 (get-in @store/app-state [:users user-id :avatar]))})
            (dom/div #js {:className "extras"}
              (om/build tags-view tags)
              (om/build new-tag-view {})
              (dom/div #js {:className "logout"
                            :onClick (fn [_] (dispatch! :logout nil))} "Log Out")))
          (apply dom/div #js {:className "threads"}
            (concat (om/build-all thread-view threads {:key :id})
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
