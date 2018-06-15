(ns braid.core.client.invites.events
  (:require
   [braid.core.client.state :refer [reg-event-fx]]
   [cljs-uuid-utils.core :as uuid]))

(defn make-invitation [data]
  {:id (or (data :id) (uuid/make-random-squuid))
   :invitee-email (data :invitee-email)
   :group-id (data :group-id)})

(reg-event-fx
  :invite
  (fn [_ [_ data]]
    (let [invite (make-invitation data)]
      {:websocket-send (list [:braid.server/invite-to-group invite])})))

(reg-event-fx
  :remove-invite
  (fn [{db :db} [_ invite]]
    {:db (update-in db [:invitations] (partial remove (partial = invite)))}))

(reg-event-fx
  :accept-invite
  (fn [{state :db} [_ invite]]
    {:websocket-send (list [:braid.server/invitation-accept invite])
     :dispatch [:remove-invite invite]}))

(reg-event-fx
  :decline-invite
  (fn [{state :db} [_ invite]]
    {:websocket-send (list [:braid.server/invitation-decline invite])
     :dispatch [:remove-invite invite]}))

(reg-event-fx
  :add-invite
  (fn [{db :db} [_ invite]]
    {:db (update-in db [:invitations] conj invite)}))

(reg-event-fx
  :generate-link
  (fn [_ [_ {:keys [group-id expires complete]}]]
    {:websocket-send
     (list
       [:braid.server/generate-invite-link {:group-id group-id :expires expires}]
       5000
       (fn [reply]
         ; indicate error if it fails?
         (when-let [link (:link reply)]
           (complete link))))}))
