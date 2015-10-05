(ns chat.client.views
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.new-message :refer [new-message-view]]
            [chat.client.views.group-invite :refer [group-invite-view]]
            [chat.client.views.helpers :as helpers])
  (:import [goog.events KeyCodes]))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message"}
        (dom/img #js {:className "avatar" :src (get-in @store/app-state [:users (message :user-id) :avatar])})
        (apply dom/div #js {:className "content"}
          (helpers/format-message (message :content)))))))

(defn thread-tags-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (let [tags (->> (thread :tag-ids)
                      (map #(get-in @store/app-state [:tags %])))]
        (apply dom/div #js {:className "tags"}
          (map (fn [tag]
                 (dom/div #js {:className "tag"
                               :style #js {:backgroundColor (helpers/tag->color tag)}}
                   (tag :name))) tags))))))

(defn thread-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (when-not (thread :new?)
          (dom/div #js {:className "close"
                        :onClick (fn [_]
                                   (dispatch! :hide-thread {:thread-id (thread :id)}))} "×"))
        (om/build thread-tags-view thread)
        (when-not (thread :new?)
          (apply dom/div #js {:className "messages"}
            (om/build-all message-view (->> (thread :messages)
                                            (sort-by :created-at))
                          {:key :id})))
        (om/build new-message-view {:thread-id (thread :id)
                                    :placeholder (if (thread :new?)
                                                   "Start a conversation..."
                                                   "Reply...")}
                  {:react-key "message"})))))

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
                      :style #js {:backgroundColor (helpers/tag->color tag)}}
          (when (tag :subscribed?)
            "✔"))
        (dom/span #js {:className "name"}
          (tag :name))))))

(defn new-tag-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:className "new-tag"
                      :onKeyDown
                      (fn [e]
                        (when (= 13 e.keyCode)
                          (let [text (.. e -target -value)]
                            (dispatch! :create-tag [text (data :group-id)]))
                          (.preventDefault e)
                          (aset (.. e -target) "value" "")))
                      :placeholder "New Tag"}))))

(defn group-tags-view [[group-id tags] owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "group"}
        (dom/h2 #js {:className "name"}
          (:name (store/id->group group-id)))
        (om/build group-invite-view group-id)
        (apply dom/div #js {:className "tags"}
          (om/build-all tag-view tags))
        (om/build new-tag-view {:group-id group-id})))))

(defn groups-view [grouped-tags owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "tag-groups"}
        (om/build-all group-tags-view grouped-tags)))))

(defn invitations-view
  [invites owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "pending-invites"}
        (dom/h2 nil "Invites")
        (apply dom/ul #js {:className "invites"}
          (map (fn [invite]
                 (dom/li #js {:className "invite"}
                   "Group "
                   (dom/strong nil (invite :group-name))
                   " from "
                   (dom/strong nil (invite :inviter-email))
                   (dom/br nil)
                   (dom/button #js {:onClick
                                    (fn [_]
                                      (dispatch! :accept-invite invite))}
                     "Accept")
                   (dom/button #js {:onClick
                                    (fn [_]
                                      (dispatch! :decline-invite invite))}
                     "Decline")))
               invites))))))

(defn chat-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [groups-map (into {} (map (juxt identity (constantly nil))) (keys (data :groups)))
            ; groups-map is just map of group-ids to nil, to be merged with
            ; tags, so there is still an entry for groups without any tags
            grouped-tags (->> (data :tags)
                              vals
                              (map (fn [tag]
                                     (assoc tag :subscribed?
                                       (store/is-subscribed-to-tag? (tag :id)))))
                              (group-by :group-id)
                              (merge groups-map))]
        (dom/div nil
          (when-let [err (data :error-msg)]
            (dom/div #js {:className "error-banner"}
              err
              (dom/span #js {:className "close"
                            :onClick (fn [_] (store/clear-error!))}
                "×")))
          (dom/div #js {:className "meta"}
            (dom/img #js {:className "avatar"
                          :src (let [user-id (get-in @store/app-state [:session :user-id])]
                                 (get-in @store/app-state [:users user-id :avatar]))})
            (dom/div #js {:className "extras"}
              (om/build groups-view grouped-tags)
              (when (seq (data :invitations))
                (om/build invitations-view (data :invitations)))
              (dom/div #js {:className "new-group"}
                (dom/label nil "New Group"
                  (dom/input #js {:placeholder "Group Name"
                                  :onKeyDown
                                  (fn [e]
                                    (when (= KeyCodes.ENTER e.keyCode)
                                      (.preventDefault e)
                                      (let [group-name (.. e -target -value)]
                                        (dispatch! :create-group {:name group-name})
                                        (set! (.. e -target -value) "")))) })))
              (dom/div #js {:className "logout"
                            :onClick (fn [_] (dispatch! :logout nil))} "Log Out")))
          (apply dom/div #js {:className "threads"}
            (concat (om/build-all thread-view
                                  (->> (vals (data :threads))
                                       (sort-by
                                         (comp (partial apply min) (partial map :created-at) :messages)))
                                  {:key :id})
                    [(om/build thread-view
                               {:id (uuid/make-random-squuid)
                                :new? true
                                :tag-ids []
                                :messages []}
                               {:react-key "new-thread"})])))))))

(defn login-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:email ""
       :password ""
       :error false})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "login"}
        (when (state :error)
          (dom/div #js {:className "error"}
            "Bad credentials, please try again"))
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
                          (dispatch! :auth
                                     {:email (state :email)
                                      :password (state :password)
                                      :on-error
                                      (fn []
                                        (om/set-state! owner :error true))}))}
          "Let's do this!")))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (if (data :session)
          (om/build chat-view data)
          (om/build login-view data))))))
