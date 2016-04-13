(ns braid.ui.view.pages.user
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.helpers :refer [user-cursor]]
            [chat.client.views.pills :refer [user-view]]))

(defn user-page-view
  [{:keys [subscribe]}]
    (fn []
      (let [user-id (subscribe [:page-id])
            current-user-id (subscribe [:user-id])
            user-avatar-url (subscribe [:user-avatar-url])
            threads (subscribe [:threads])
            open-threads-ids (subscribe [:open-thread-ids])
            open-threads (select-keys @threads @open-threads-ids)
            user (subscribe [:user @user-id])]
        [:div.page.channel
          [:div.title
            [user-view @user subscribe]]
          [:div.content
            [:div.description
              [:img.avatar {:src @user-avatar-url}]
              [:p "One day, a profile will be here."]
              [:p "Currently only showing your open threads that mention this user."]
              [:p "Soon, you will see all recent threads this user has participated in."]]]
          [:div.threads {:ref "threads-div"
                         :on-wheel
                         (fn [e]
                           (let [target-classes (.. e -target -classList)
                                 this-elt (.. e -target)]
                             ; TODO: check if threads-div needs to scroll?
                             (when (and (or (.contains target-classes "thread")
                                            (.contains target-classes "threads"))
                                     (= 0 (.-deltaX e) (.-deltaZ e)))
                               (set! (.-scrollLeft this-elt)
                                     (- (.-scrollLeft this-elt) (.-deltaY e))))))}
             (let [sorted-threads (->> @open-threads-ids
                                  ; sort by last message sent by logged-in user, most recent first
                                        vals
                                        (filter
                                          (fn [thread]
                                            (contains?
                                              (set (thread :mentioned-ids)) @user-id)))
                                        (sort-by
                                          (comp (partial apply max)
                                                (partial map :created-at)
                                                (partial filter
                                                  (fn [m] (= (m :user-id) @current-user-id)))
                                                :messages))
                                          reverse)]
                [new-thread-view]
                (for [thread @sorted-threads]
                  [thread-view thread]))]])))

;(om/get-node owner "threads-div")
; [new-thread-view {:mentioned-ids [user-id]}]
