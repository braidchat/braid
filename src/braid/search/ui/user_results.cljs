(ns braid.search.ui.user-results
  (:require
   [braid.core.client.ui.views.user-hover-card :as user-card]))

(defn search-users-view
  [_ users]
  [:<>
   [:div.content
    [:div.description
     (count users)]]
   [:div.results
    [:div.users
     (doall
       (for [{:keys [user-id]} users]
         ^{:key user-id}
         [user-card/user-hover-card-view user-id]))]]])
