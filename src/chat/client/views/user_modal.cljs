(ns chat.client.views.user-modal
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.group-invite :refer [group-invite-view]]
            [chat.client.views.pills :refer [id->color]]
            [chat.shared.util :refer [valid-tag-name? valid-nickname?]])
  (:import [goog.events KeyCodes]))

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
                      :style #js {:backgroundColor (id->color (tag :id))}}
          (when (tag :subscribed?)
            "✔"))
        (dom/span #js {:className "name"}
          (tag :name))))))


(defn nickname-view [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:error nil
       :format-error false})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "nickname"}
        (when-let [current (data :nickname)]
          (dom/div #js {:className "current-name"} current))
        (when-let [msg (state :error)]
          (dom/span #js {:className "error"} msg))
        ; TODO: check if nickname is taken while typing
        (dom/input
          #js {:className (str "new-name " (when (state :format-error) "error"))
               :placeholder "New Nickname"
               :onKeyUp
               (fn [e]
                 (let [text (.. e -target -value)]
                   (om/set-state! owner :format-error (not (valid-nickname? text)))))
               :onKeyDown
               (fn [e]
                 (om/set-state! owner :error nil)
                 (let [nickname (.. e -target -value)]
                   (when (and (= KeyCodes.ENTER e.keyCode) (re-matches #"\S+" nickname))
                     (dispatch! :set-nickname [nickname (fn [err] (om/set-state! owner :error err))]))))})))))

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

(defn user-modal-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:active? false})
    om/IRenderState
    (render-state [_ {:keys [active?] :as state}]
      (dom/div #js {:className "meta"}
        (dom/img #js {:className "avatar"
                      :onClick (fn [e]
                                 (om/update-state! owner :active? not))
                      :src (let [user-id (get-in @store/app-state [:session :user-id])]
                             (get-in @store/app-state [:users user-id :avatar]))})
        (dom/div #js {:className (str "user-modal " (when active? "active"))}
          (dom/div #js {:className "close"
                        :onClick (fn [_]
                                   (om/set-state! owner :active? false))} "×")
          (om/build nickname-view (data :session))
          (apply dom/div nil
            (map
              (fn [group]
                (dom/div #js {}
                  (dom/div nil (group :name))
                  (om/build group-invite-view (group :id))))
              (vals (data :groups))))

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
                        :onClick (fn [_] (dispatch! :logout nil))} "Log Out"))))))
