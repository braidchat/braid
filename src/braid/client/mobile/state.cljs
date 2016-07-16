(ns braid.client.mobile.state
  (:require [re-frame.core :as rf]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.mobile.remote :refer [fake-response]]
            [braid.client.state :as state]))

; login

(rf/register-handler :log-in!
  (fn [state _]
    (merge state fake-response)))

(rf/register-sub :logged-in?
  (fn [state _]
    (reaction (boolean (:user @state)))))

(comment ; XXX: new state uses multimethod
 (rf/register-sub :groups state/get-groups)

 (rf/register-sub :group-unread-count state/get-group-unread-count)


 ; current group

 (rf/register-sub :active-group state/get-active-group)
 )

(rf/register-sub :active-group-inbox-threads
  (fn [state _]
    (let [group-id (reaction (:open-group-id @state))
          threads (reaction (vals (:threads @state)))]
      (reaction (filter (fn [t] (= @group-id (t :group-id)))
                        @threads)))))


; tag

(rf/register-sub :get-tag
  (fn [state [_ tag-id]]
    (reaction (get-in @state [:tags tag-id]))))
