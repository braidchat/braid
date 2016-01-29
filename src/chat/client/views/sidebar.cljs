(ns chat.client.views.sidebar
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.pills :refer [tag-view user-view]]
            ))

(defn sidebar-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "sidebar"}

        (dom/h2 #js {:className "inbox link"
                     :onClick (fn []
                                (store/set-page! {:type :inbox}))}
          "Inbox"
          (dom/span #js {:className "count"}
            (count (get-in @store/app-state [:user :open-thread-ids]))))
        (dom/h2 #js {:className "channels link"
                     :onClick (fn []
                                (store/set-page! {:type :channels}))}
          "Channels")
        (dom/div #js {:className "conversations"}
          (apply dom/div nil
            (->> (@store/app-state :tags)
                 vals
                 (filter (fn [t] (= (@store/app-state :open-group-id) (t :group-id))))
                 (filter (fn [t] (store/is-subscribed-to-tag? (t :id))))
                 (sort-by :threads-count)
                 reverse
                 (take 8)
                 (map (fn [tag]
                        (dom/div nil (om/build tag-view tag)))))))

        (comment
          (dom/h2 nil "Direct Messages")
          ; TODO: when this comes back, need to observe the user cursor
          (apply dom/div #js {:className "users"}
            (->> (@store/app-state :users)
                 vals
                 (remove (fn [user] (= (get-in @store/app-state [:session :user-id]) (user :id))))
                 (map (fn [user]
                        (dom/div nil
                          (om/build user-view user)))))))

        (dom/h2 nil "Recommended")
        (apply dom/div #js {:className "recommended"}
          (->> (@store/app-state :tags)
               vals
               (filter (fn [t] (= (@store/app-state :open-group-id) (t :group-id))))
               (remove (fn [t] (store/is-subscribed-to-tag? (t :id))))
               (sort-by :threads-count)
               reverse
               (take 2)
               ; shuffle
               (map (fn [tag]
                      (dom/div nil (om/build tag-view tag))))))

        ))))
