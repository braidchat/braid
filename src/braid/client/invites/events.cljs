(ns braid.client.invites.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [cljs-uuid-utils.core :as uuid]))

(defn make-invitation [data]
  {:id (or (data :id) (uuid/make-random-squuid))
   :invitee-email (data :invitee-email)
   :group-id (data :group-id)})

(reg-event-fx
  :invite
  (fn [cofx [_ data]]
    (let [invite (make-invitation data)]
      {:websocket-send (list [:braid.server/invite-to-group invite])})))

(reg-event-db
  :remove-invite
  (fn [state [_ invite]]
    (update-in state [:invitations] (partial remove (partial = invite)))))

(reg-event-fx
  :accept-invite
  (fn [{state :db :as cofx} [_ invite]]
    {:websocket-send (list [:braid.server/invitation-accept invite])
     :dispatch [:remove-invite invite]}))

(reg-event-fx
  :decline-invite
  (fn [{state :db :as cofx} [_ invite]]
    {:websocket-send (list [:braid.server/invitation-decline invite])
     :dispatch [:remove-invite invite]}))

(reg-event-db
  :add-invite
  (fn [state [_ invite]]
    (update-in state [:invitations] conj invite)))

(reg-event-fx
  :generate-link
  (fn [cofx [_ {:keys [group-id expires complete]}]]
    {:websocket-send
     (list
       [:braid.server/generate-invite-link {:group-id group-id :expires expires}]
       5000
       (fn [reply]
         ; indicate error if it fails?
         (when-let [link (:link reply)]
           (complete link))))}))
