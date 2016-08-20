(ns braid.client.calls.helpers)

(defn add-call [state call]
  (update-in state [:calls] #(assoc % (:id call) call)))

(defn set-call-status [state call-id status]
  (assoc-in state [:calls call-id :status] status))
