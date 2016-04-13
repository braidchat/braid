(ns braid.ui.views.pages.me
  (:require [om.core :as om]
            [om.dom :as dom]
            [reagent.core :as r]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.pills :refer [user-view]]
            [chat.shared.util :refer [valid-tag-name? valid-nickname?]])
  (:import [goog.events KeyCodes]))

(defn nickname-view
  [{:keys [subscribe]}]
  (let [format-error (r/atom false)
        error (r/atom nil)
        set-format-error! (fn [error?] (swap! format-error error?))
        set-error! (fn [err] (swap! error err))
        nickname (subscribe [:nickname])]
    (fn []
      [:div.nickname
        (when-let [current @nickname]
          [:div.current-name current])
        (when-let [msg @error]
          [:span.error msg])
        ; TODO: check if nickname is taken while typing
        [:input.new-name
          {:class (when @format-error "error")
           :placeholder "New Nickname"
           :on-keyup
             (fn [e]
               (let [text (.. e -target -value)]
                 (set-format-error! (not (valid-nickname? text)))))
           :on-keydown
           (fn [e]
             (set-error! nil)
             (let [nickname (.. e -target -value)]
               (when (and (= KeyCodes.ENTER e.keyCode)
                          (re-matches #"\S+" nickname))
                 (dispatch! :set-nickname
                            [nickname
                              (fn [err] (set-error! err))]))))}]])))

(defn invitations-view
  [invites]
  (fn []
    [:div.pending-invites
      [:h2 "Invites"]
      [:ul.invites
         (for [invite invites]
           [:li.invite
             "Group "
             [:strong (invite :group-name)]
             " from "
             [:strong (invite :inviter-email)]
             [:br]
             [:button {:on-click
                        (fn [_]
                          (dispatch! :accept-invite invite))}
               "Accept"]
             (:button {:on-click
                        (fn [_]
                          (dispatch! :decline-invite invite))}
               "Decline")]
           )]]))

(defn me-page-view-test
  [{:keys [subscribe]}]
  (let [invitations (subscribe [:invitations])]
    (fn []
      [:div.page.me
        [:div.title "Me!"]
        [:div.content
          [:h2 "Log Out"]
          [:button.logout {:on-click
                            (fn [_]
                              (dispatch! :logout nil))} "Log Out"]
          [:h2 "Update Nickname"]
          [nickname-view subscribe]
          [:h2 "Received Invites"]
          (when (seq @invitations)
            ;TODO: render correctly
            [invitations-view @invitations])]])))
