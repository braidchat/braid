(ns braid.search.ui.user-results
  (:require
   [braid.core.client.ui.views.user-hover-card :as user-card]
   [braid.core.client.ui.styles.hover-cards :as card-styles]))

(defn search-users-view
  [_ users]
  [:div.result.user
   [:div.description
    (str (count users) " user" (when (not= (count users) 1) "s"))]
   [:div.users.content
    (doall
      (for [{:keys [user-id]} users]
        ^{:key user-id}
        [user-card/user-hover-card-view user-id]))]])

(def styles
  [:>.result.user
   [:>.users.content
    ;; would like to just do "flex-wrap: wrap" but apparently a
    ;; flex-direction: column + flex-wrap: wrap doesn't grow the width
    ;; of the parent when it wraps, so then this ends up overlapping
    ;; with the search results next to it
    {:display "flex"
     :flex-direction "column"
     :align-items "flex-end"
     :max-height "90%"
     :gap "0.5rem"
     :margin-left "0.5rem"
     :overflow-y "auto"
     :padding "0.25rem"}
    card-styles/>user-card
    [:>.card.user
     {:min-height "5rem"
      :margin-top 0
      :margin-left "0.5rem"}]]])
