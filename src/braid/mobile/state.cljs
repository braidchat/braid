(ns braid.mobile.state
  (:require [re-frame.core :as rf]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.mobile.remote :refer [fake-response]]
            ))

; login

(rf/register-handler :log-in!
  (fn [state _]
    (merge state fake-response)))

(rf/register-sub :logged-in?
  (fn [state _]
    (reaction (boolean (:user @state)))))

(rf/register-sub :groups-with-unread
  (fn [state _]
    (let [groups (reaction (vals (:groups @state)))
          threads-by-group-id (->> (:threads @state)
                                   vals
                                   (group-by :group-id))]
      ; TODO not actually unread-count
      (reaction (->> @groups
                     (map (fn [g]
                            (assoc g :unread-count
                              (count (threads-by-group-id (g :id)))))))))))
; current group

(rf/register-sub :active-group
  (fn [state _]
    (let [group-id (reaction (:active-group-id @state))]
      (reaction (get-in @state [:groups @group-id])))))

(rf/register-sub :active-group-inbox-threads
  (fn [state _]
    (let [group-id (reaction (:active-group-id @state))
          threads (reaction (vals (:threads @state)))]
      (reaction (filter (fn [t] (= @group-id (t :group-id)))
                        @threads)))))

(rf/register-handler :set-active-group-id!
  (fn [state [_ group-id]]
    (assoc state :active-group-id group-id)))

; tag

(rf/register-sub :get-tag
  (fn [state [_ tag-id]]
    (reaction (get-in @state [:tags tag-id]))))
