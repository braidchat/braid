(ns braid.ui.views.group-invite
  (:require [om.core :as om]
            [om.dom :as dom]
            [reagent.core :as r]
            [clojure.string :as string]
            [chat.client.dispatcher :refer [dispatch!]])
  (:import [goog.events KeyCodes]))

(defn group-invite-view
  [group-id]
  (let [collapsed? (r/atom true)
        invitee-email (r/atom "")
        set-collapse! (fn [c?] (reset! collapsed? c?))
        set-invitee-email! (fn [message] (reset! invitee-email message))]
    (fn []
      (if collapsed?
        [:button.invite-open {:on-click
                                (fn [_]
                                    (set-collapse! false))}
          "invite"]
        [:div.invite-form
          [:input {:value invitee-email
                   :type "email"
                   :placeholder "email address"
                   :on-change
                     (fn [e]
                       (set-invitee-email! (.. e -target -value)))}]
          [:button.invite {:disabled (string/blank? invitee-email)
                           :on-click
                             (fn [_]
                               (dispatch! :invite {:group-id group-id
                                                   :invitee-email invitee-email})
                               (set-collapse! true)
                               (set-invitee-email! ""))}
            "invite"]
          [:button.close {:on-click
                           (fn [_]
                             (set-collapse! true))}
            "cancel"]]))))


