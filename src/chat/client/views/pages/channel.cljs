(ns chat.client.views.pages.channel
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.pills :refer [tag-view]]))

(defn channel-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [tag-id (get-in @store/app-state [:page :id])
            tag (get-in @store/app-state [:tags tag-id])]
        (dom/div #js {:className "page channel"}
          (dom/div #js {:className "title"}
            (om/build tag-view tag)
            (if (store/is-subscribed-to-tag? (tag :id))
              (dom/a #js {:className "button"
                          :onClick (fn [_]
                                     (dispatch! :unsubscribe-from-tag (tag :id)))}
                "Unsubscribe")
              (dom/a #js {:className "button"
                          :onClick (fn [_]
                                     (dispatch! :subscribe-to-tag (tag :id)))}
                "Subscribe")))

          (dom/div #js {:className "description"}
            (dom/p nil "One day, a channel description will be here.")
            (dom/p nil "Currently only showing your open threads for this channel.")
            (dom/p nil "Soon, you will see all recent threads for this channel."))

          (apply dom/div #js {:className "threads"}
            (concat
              [(new-thread-view {:tag-ids [tag-id]})]
              (map (fn [t] (om/build thread-view t {:key :id}))
                   (let [user-id (get-in @store/app-state [:session :user-id])]
                     ; sort by last message sent by logged-in user, most recent first
                     (->> (select-keys (data :threads) (get-in data [:user :open-thread-ids]))
                          vals
                          (filter (fn [thread]
                                    (contains? (set (thread :tag-ids)) tag-id)))
                          (sort-by
                            (comp (partial apply max)
                                  (partial map :created-at)
                                  (partial filter (fn [m] (= (m :user-id) user-id)))
                                  :messages))
                          reverse))))))))))
