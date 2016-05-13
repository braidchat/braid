(ns braid.ui.views.pages.user
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.ui.views.call :refer [caller-tag-view]]
            [braid.ui.views.pills :refer [user-pill-view]]
            [braid.ui.views.new-thread :refer [new-thread-view]]
            [braid.ui.views.threads :refer [threads-view]]
            [chat.client.reagent-adapter :refer [subscribe]]))

(defn user-page-view
  []
  (let [user-id (subscribe [:page-id])
        user (subscribe [:user] [user-id])
        current-user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url])
        threads (subscribe [:threads])
        open-threads-ids (subscribe [:open-thread-ids])
        open-threads (reaction (select-keys @threads @open-threads-ids))
        sorted-threads (reaction (->> @open-threads
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
                                      reverse))]

    (fn []
      [:div.page.channel
       [:div.title
        [user-pill-view (@user :id)]]
       [:div.content
        [:div.description
         [:img.avatar {:src @user-avatar-url}]
         [:p "One day, a profile will be here."]
         [:p "Currently only showing your open threads that mention this user."]
         [:p "Soon, you will see all recent threads this user has participated in."]
         (when (not= @current-user-id (@user :id))
           [caller-tag-view (@user :id)])]]
       [threads-view {:new-thread-args {:mentioned-ids [(@user :id)]}
                      :threads @sorted-threads}]])))
