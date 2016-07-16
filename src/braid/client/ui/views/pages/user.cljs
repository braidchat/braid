(ns braid.client.ui.views.pages.user
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.ui.views.pills :refer [user-pill-view]]
            [braid.client.ui.views.threads :refer [threads-view]]
            [braid.client.state :refer [subscribe]]))

(defn user-page-view
  []
  (let [user-id (subscribe [:page-id])
        user (subscribe [:user] [user-id])
        current-user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url] [user-id])
        threads (subscribe [:threads])
        open-threads-ids (subscribe [:open-thread-ids])
        open-group (subscribe [:open-group-id])
        open-threads (reaction (select-keys @threads @open-threads-ids))
        sorted-threads (reaction (->> @open-threads
                                      ; sort by last message sent by logged-in user, most recent first
                                      vals
                                      (filter
                                        (fn [thread]
                                          (and
                                            (= (thread :group-id) @open-group)
                                            (contains?
                                              (set (thread :mentioned-ids)) @user-id))))
                                      (sort-by
                                        (comp (partial apply max)
                                              (partial map :created-at)
                                              (partial filter
                                                       (fn [m] (= (m :user-id) @current-user-id)))
                                              :messages))
                                      reverse))]

    (fn []
      [:div.page.userpage
       [:div.title
        [user-pill-view (@user :id)]]
       [:div.content
        [:div.description
         [:img.avatar {:src @user-avatar-url}]
         [:p "One day, a profile will be here."]
         [:p "Currently only showing your open threads that mention this user in this group."]
         [:p "Soon, you will see all recent threads this user has participated in."]]]
       [threads-view {:new-thread-args {:mentioned-ids [(@user :id)]}
                      :threads @sorted-threads}]])))
