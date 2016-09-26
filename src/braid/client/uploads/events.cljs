(ns braid.client.uploads.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
            [cljs-uuid-utils.core :as uuid]))

(defn make-upload [data]
  (merge {:id (uuid/make-random-squuid)}
         data))

(reg-event-fx
  :create-upload
  (fn [{state :db :as cofx} [_ {:keys [url thread-id group-id]}]]
    {:websocket-send (list [:braid.server/create-upload
                            (make-upload {:url url :thread-id thread-id})])
     :dispatch [:new-message {:content url
                              :thread-id thread-id
                              :group-id group-id}]}))

(reg-event-fx
  :get-group-uploads
  (fn [cofx [_ {:keys [group-id on-success on-error]}]]
    {:websocket-send
     (list
       [:braid.server/uploads-in-group group-id]
       5000
       (fn [reply]
         (if-let [uploads (:braid/ok reply)]
           (on-success uploads)
           (on-error (get reply :braid/error "Couldn't get uploads in group")))))}))
