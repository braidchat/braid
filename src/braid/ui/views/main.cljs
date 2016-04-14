(ns braid.ui.views.main
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.store :as store]
            [braid.ui.pages.inbox :refer [inbox-page-view]]
            [braid.ui.pages.recent :refer [recent-page-view]]
            [braid.ui.pages.help :refer [help-page-view]]
            [braid.ui.pages.users :refer [users-page-view]]
            [braid.ui.pages.search :refer [search-page-view]]
            [braid.ui.pages.tag :refer [tag-page-view]]
            [braid.ui.pages.me :refer [me-page-view]]
            [braid.ui.pages.groups :refer [groups-page-view]]
            [braid.ui.pages.extensions :refer [extensions-page-view]]))

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
         :tag [tag-page-view]
         :user [user-page-view]
         :tags [tags-page-view]
         :me [me-page-view]
         :group-explore [group-explore-view]
         :extensions [extensions-page-view])])))
