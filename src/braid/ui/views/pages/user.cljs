(ns braid.ui.view.pages.user
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.helpers :refer [user-cursor]]
            [chat.client.views.pills :refer [user-view]]))

(defn user-page-view
  [data subscribe]
    (fn []
      (let [user-id (subscribe [:page-id])
            current-user-id (subscribe [:user-id])
            threads (data :threads)
            user (om/observe owner (user-cursor user-id))]
        [:div.page.channel
          [:div.title
            [(user-view user subscribe)]]
          [:div.content
            [:div.description
              [:img.avatar {:src (user :avatar)}]
              [:p "One day, a profile will be here."]
              [:p "Currently only showing your open threads that mention this user."]
              [:p "Soon, you will see all recent threads this user has participated in."]]]
          [:div.threads {:ref "threads-div"
                         :on-wheel
                         (fn [e]
                           (let [target-classes (.. e -target -classList)
                                 this-elt (om/get-node owner "threads-div")]
                             ; TODO: check if threads-div needs to scroll?
                             (when (and (or (.contains target-classes "thread")
                                            (.contains target-classes "threads"))
                                     (= 0 (.-deltaX e) (.-deltaZ e)))
                               (set! (.-scrollLeft this-elt)
                                     (- (.-scrollLeft this-elt) (.-deltaY e))))))}
            [(new-thread-view {:mentioned-ids [user-id]})]
            (for [thread threads]
              [(thread-view thread {:key :id})])
               ; sort by last message sent by logged-in user, most recent first
               (->> (select-keys (data :threads) (get-in data [:user :open-thread-ids]))
                    vals
                    (filter (fn [thread]
                              (contains? (set (thread :mentioned-ids)) @user-id)))
                    (sort-by
                      (comp (partial apply max)
                            (partial map :created-at)
                            (partial filter (fn [m] (= (m :user-id) @current-user-id)))
                            :messages))
                    reverse)]])))
