(ns chat.client.views.pages.me
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.group-invite :refer [group-invite-view]]
            [chat.client.views.pills :refer [user-view]]
            [chat.shared.util :refer [valid-tag-name? valid-nickname?]])
  (:import [goog.events KeyCodes]))

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

(defn me-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page me"}
        (dom/div #js {:className "title"} "Me!")



        (dom/div #js {:className "content"}

          (dom/h2 nil "Log Out")
          (dom/button #js {:className "logout"
                           :onClick (fn [_] (dispatch! :logout nil))} "Log Out")

          (dom/h2 nil "Update Nickname")
          (om/build nickname-view (data :session))

          (dom/h2 nil "Send Invites")
          (apply dom/div nil
            (map
              (fn [group]
                (dom/div #js {}
                  (dom/div nil (group :name))
                  (om/build group-invite-view (group :id))))
              (vals (data :groups))))

          (dom/h2 nil "Received Invites")
          (when (seq (data :invitations))
            (om/build invitations-view (data :invitations))))))))
