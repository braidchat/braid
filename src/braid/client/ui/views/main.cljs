(ns braid.client.ui.views.main
  (:require [re-frame.core :refer [subscribe]]
            [braid.client.routes :as routes]
            [braid.client.ui.views.error-banner :refer [error-banner-view]]
            [braid.client.ui.views.sidebar :refer [sidebar-view]]
            [braid.client.ui.views.header :refer [header-view]]
            [braid.client.ui.views.pages.inbox :refer [inbox-page-view]]
            [braid.client.ui.views.pages.recent :refer [recent-page-view]]
            [braid.client.ui.views.pages.search :refer [search-page-view]]
            [braid.client.ui.views.pages.tags :refer [tags-page-view]]
            [braid.client.ui.views.pages.me :refer [me-page-view]]
            [braid.client.ui.views.pages.invite :refer [invite-page-view]]
            [braid.client.ui.views.pages.group-explore :refer [group-explore-page-view]]
            [braid.client.ui.views.pages.global-settings :refer [global-settings-page-view]]
            [braid.client.ui.views.pages.group-settings :refer [group-settings-view]]
            [braid.client.ui.views.pages.bots :refer [bots-view]]
            [braid.client.ui.views.pages.uploads :refer [uploads-view]]
            [braid.client.ui.views.pages.thread :refer [single-thread-view]]
            [braid.client.ui.views.pages.changelog :refer [changelog-view]]))

(defn page-view []
  (let [page (subscribe [:page])]
    (fn []
      (case (@page :type)
        :inbox [inbox-page-view]
        :recent [recent-page-view]
        :search [search-page-view]
        :tags [tags-page-view]
        :me [me-page-view]
        :invite [invite-page-view]
        :group-explore [group-explore-page-view]
        :bots [bots-view]
        :uploads [uploads-view]
        :thread [single-thread-view]
        :settings [group-settings-view]
        :global-settings [global-settings-page-view]
        :changelog [changelog-view]
        :index (do (when-let [group-id (-> @(subscribe [:ordered-groups])
                                           first
                                           :id)]
                     (routes/go-to! (routes/inbox-page-path {:group-id group-id})))
                   [:h1 "Redirecting..."])
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
