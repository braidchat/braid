(ns braid.core.client.invites.views.invite
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch subscribe]])
  (:import
   (goog.events KeyCodes)))

(defn invite-view
  []
  (let [group-id (subscribe [:open-group-id])
        collapsed? (r/atom true)
        invitee-email (r/atom "")
        set-collapse! (fn [c?] (reset! collapsed? c?))
        set-invitee-email! (fn [message] (reset! invitee-email message))
        link-expires (r/atom :day)
        invite-link (r/atom nil)]
    (fn []
      [:div.invite
       [:div.by-link
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
                                [:generate-link!
                                 {:group-id @group-id
                                  :expires @link-expires
                                  :complete (fn [link] (reset! invite-link link))}]))}
         "Generate"]
        (when (= @link-expires :never)
          [:p.warning
           "Be careful with never-expiring links! They will allow anyone with "
           "this link to always be able to join, even if they get removed from "
           "the group. Prefer to use one of the expiring links instead!"])
        (when @invite-link
          [:div.invite-link
           [:input
            {:type "text"
             :on-focus (fn [e] (.. e -target select))
             :read-only true
             :value @invite-link}]])]
       [:div.by-email
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
                              (dispatch [:invite! {:group-id @group-id
                                                  :invitee-email @invitee-email}])
                              (set-collapse! true)
                              (set-invitee-email! ""))}
            "Send Invite"]
           [:button.close {:on-click
                           (fn [_]
                             (set-collapse! true))}
            "Cancel"]])]])))
