(ns braid.ui.views.main
  (:require [chat.client.reagent-adapter :refer [subscribe]]))

(defn error-banner-view []
  (let [errors (subscribe [:errors])]
    (fn []
      [:div.error-banners
       (for [[err-key err] @errors]
         [:div.error-banner
          err
          [:span.close
           {:on-click (fn [_] (store/clear-error! err-key))}
           "×"]])])))

(defn main-view []
  (let [page (subscribe [:page])]
    (fn [_]
      [:div.main
       [error-banner-view]
       [sidebar-view]
       [header-view]
       (case (@page :type)
         :inbox [inbox-page-view]
         :recent [recent-page-view]
         :help [help-page-view]
         :users [users-page-view]
         :search [search-page-view]
         :channel [channel-page-view]
         :user [user-page-view]
         :channels [channels-page-view]
         :me [me-page-view]
         :group-explore [group-explore-view]
         :extensions [extensions-page-view])])))
