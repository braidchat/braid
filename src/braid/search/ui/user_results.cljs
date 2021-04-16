(ns braid.search.ui.user-results
  (:require
   [braid.core.client.ui.views.user-hover-card :as user-card]
   [braid.core.client.ui.styles.hover-cards :as card-styles]))

(defn search-users-view
  [_ users]
  [:div.result.user
   [:div.description
    (str (count users) " users")]
   [:div.users.content
    (doall
      (for [{:keys [user-id]} users]
        ^{:key user-id}
        [user-card/user-hover-card-view user-id]))]])

(def styles
  [:>.result.user
   [:>.users.content
    {:display "flex"
     :flex-direction "row"
     :align-items "flex-end"
     :gap "0.5rem"}
    card-styles/>user-card]])
