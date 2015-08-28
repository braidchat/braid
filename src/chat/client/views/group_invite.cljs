(ns chat.client.views.group-invite
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs.core.async :refer [put!]]
            [om-fields.core :refer [input]]
            [chat.client.store :as store])
  (:import [goog.events KeyCodes]))

(defn group-invite-view
  [group-id owner]
  (reify
    om/IInitState
    (init-state [_]
      {:collapsed? true
       :invitee-email ""})
    om/IRenderState
    (render-state [_ {:keys [collapsed? invitee-email]}]
      (if collapsed?
        (dom/span #js {:className "invite-open"
                       :onClick (fn [_]
                                  (om/set-state! owner :collapsed? false))}
          "invite")
        (dom/div #js {:className "invite-form"}
          (om/build input {:value invitee-email}
                    {:opts {:type :autocomplete
                            :edit-key [:value]
                            :update-fn (fn [v]
                                         (om/set-state! owner :invitee-email (str v)))
                            :placeholder "User to invite"
                            :search-fn
                            (fn [s ch]
                              (let [emails (->> (store/get-user-emails)
                                                (filter #(not= -1 (.indexOf % s))))]
                                (put! ch emails)))
                            :choice-render-fn str
                            :class "autocomplete"}})
          (dom/span #js {:className "invite-close"
                         :onClick (fn [_]
                                    (om/set-state! owner :collapsed? true))}
            "cancel"))))))

