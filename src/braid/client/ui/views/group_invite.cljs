(ns braid.client.ui.views.group-invite
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [re-frame.core :refer [dispatch subscribe]])
  (:import [goog.events KeyCodes]))

(defn group-invite-view
  []
  (let [group-id (subscribe [:open-group-id])
        collapsed? (r/atom true)
        invitee-email (r/atom "")
        set-collapse! (fn [c?] (reset! collapsed? c?))
        set-invitee-email! (fn [message] (reset! invitee-email message))
        link-expires (r/atom :never)
        invite-link (r/atom nil)]
    (fn []
      [:div
       [:div
        [:h2 "Invite by link"]
        [:p "Anyone with this link will be able to join"]
        [:label "Link expires"
         [:select {:value @link-expires
                   :on-change (fn [e]
                                (let [v (keyword (.. e -target -value))]
                                  (reset! link-expires v)))}
          (conj
            (for [exp [:day :month :week]]
              ^{:key exp}
              [:option {:value exp} (str "In one " (name exp))])
            ^{:key :never}
            [:option {:value :never} "Never"])]]
        [:button {:on-click (fn []
                              (dispatch
                                [:generate-link
                                 {:group-id @group-id
                                  :expires @link-expires
                                  :complete (fn [link] (reset! invite-link link))}]))}
         "Generate"]
        (when @invite-link
          [:input {:type "text"
                   :on-focus (fn [e] (.. e -target select))
                   :read-only true
                   :value @invite-link}])]
       [:div
        [:h2 "Invite By Email"]
        (if @collapsed?
          [:button.invite-open {:on-click
                                (fn [_]
                                  (set-collapse! false))}
           "Invite"]
          [:div.invite-form
           [:input {:value @invitee-email
                    :type "email"
                    :placeholder "email address"
                    :on-change
                    (fn [e]
                      (set-invitee-email! (.. e -target -value)))}]
           [:button.invite {:disabled (string/blank? @invitee-email)
                            :on-click
                            (fn [_]
                              (dispatch [:invite {:group-id @group-id
                                                  :invitee-email @invitee-email}])
                              (set-collapse! true)
                              (set-invitee-email! ""))}
            "invite"]
           [:button.close {:on-click
                           (fn [_]
                             (set-collapse! true))}
            "cancel"]])]])))


