(ns braid.ui.views.pages.me
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.shared.util :refer [valid-nickname?]])
  (:import [goog.events KeyCodes]))

(defn nickname-view
  []
  (let [format-error (r/atom false)
        error (r/atom nil)
        set-format-error! (fn [error?] (reset! format-error error?))
        set-error! (fn [err] (reset! error err))
        user-id (subscribe [:user-id])
        nickname (subscribe [:nickname @user-id])]
    (fn []
      [:div.nickname
       (when @nickname
         [:div.current-name @nickname])
       (when @error
         [:span.error @error])
       ; TODO: check if nickname is taken while typing
       [:input.new-name
        {:class (when @format-error "error")
         :placeholder "New Nickname"
         :on-key-up
         (fn [e]
           (let [text (.. e -target -value)]
             (set-format-error! (not (valid-nickname? text)))))
         :on-key-down
         (fn [e]
           (set-error! nil)
           (let [nickname (.. e -target -value)]
             (when (and (= KeyCodes.ENTER e.keyCode)
                     (re-matches #"\S+" nickname))
               (dispatch! :set-nickname
                          [nickname
                           (fn [err] (set-error! err))]))))}]])))

(defn password-view
  []
  (let [new-pass (r/atom "")
        pass-confirm (r/atom "")
        response (r/atom nil)]
    (fn []
      [:form.password
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (.stopPropagation e)
                     (when (and (not (string/blank? @new-pass))
                             (= @new-pass @pass-confirm))
                       (dispatch! :set-password
                                  [@new-pass
                                   (fn []
                                     (reset! response {:ok true})
                                     (reset! new-pass "")
                                     (reset! pass-confirm ""))
                                   (fn [err]
                                     (reset! response err))])))}
       (when @response
         (if-let [err (:error @response)]
           [:div.error err]
           [:div.success "Password changed"]))
       [:label "New Password"
        [:input.new-password
         {:type "password"
          :placeholder "••••••••"
          :value @new-pass
          :on-change (fn [e] (reset! new-pass (.. e -target -value)))}]]
       [:label "Confirm Password"
        [:input.new-password
         {:type "password"
          :placeholder "••••••••"
          :value @pass-confirm
          :on-change (fn [e] (reset! pass-confirm (.. e -target -value)))}]]
       [:button {:disabled (or (string/blank? @new-pass)
                               (not= @new-pass @pass-confirm))}
        "Change Password"]])))

(defn invitations-view
  [invites]
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
       [:button {:on-click
                 (fn [_]
                   (dispatch! :decline-invite invite))}
        "Decline"]])]])

(defn me-page-view
  []
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
        [nickname-view]
        [:h2 "Change Password"]
        [password-view]
        [:h2 "Received Invites"]
        (when (seq @invitations)
          ;TODO: render correctly
          [invitations-view @invitations])]])))
