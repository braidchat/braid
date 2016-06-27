(ns braid.ui.views.main
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.routes :as routes]
            [braid.ui.views.error-banner :refer [error-banner-view]]
            [braid.ui.views.sidebar :refer [sidebar-view]]
            [braid.ui.views.header :refer [header-view]]
            [braid.ui.views.pages.inbox :refer [inbox-page-view]]
            [braid.ui.views.pages.recent :refer [recent-page-view]]
            [braid.ui.views.pages.users :refer [users-page-view]]
            [braid.ui.views.pages.user :refer [user-page-view]]
            [braid.ui.views.pages.search :refer [search-page-view]]
            [braid.ui.views.pages.tag :refer [tag-page-view]]
            [braid.ui.views.pages.tags :refer [tags-page-view]]
            [braid.ui.views.pages.me :refer [me-page-view]]
            [braid.ui.views.pages.invite :refer [invite-page-view]]
            [braid.ui.views.pages.group-explore :refer [group-explore-page-view]]
            [braid.ui.views.pages.global-settings :refer [global-settings-page-view]]
            [braid.ui.views.pages.group-settings :refer [group-settings-view]]
            [braid.ui.views.pages.bots :refer [bots-view]]
            [braid.ui.views.pages.uploads :refer [uploads-view]]
            [braid.ui.views.pages.thread :refer [single-thread-view]]))

(defn page-view []
  (let [page (subscribe [:page])]
    (fn []
      (case (@page :type)
        :inbox [inbox-page-view]
        :recent [recent-page-view]
        :users [users-page-view]
        :search [search-page-view]
        :tag [tag-page-view]
        :user [user-page-view]
        :tags [tags-page-view]
        :me [me-page-view]
        :invite [invite-page-view]
        :group-explore [group-explore-page-view]
        :bots [bots-view]
        :uploads [uploads-view]
        :thread [single-thread-view]
        :settings [group-settings-view]
        :global-settings [global-settings-page-view]
        (do (routes/go-to! (routes/index-path))
            [:h1 "???"])))))

(defn main-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      [:div.main
       [error-banner-view]
       [sidebar-view]
       (when @group-id
         [header-view])
       [page-view]])))
