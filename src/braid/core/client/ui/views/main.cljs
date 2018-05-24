(ns braid.core.client.ui.views.main
  (:require
   [braid.core.client.bots.views.bots-page :refer [bots-page-view]]
   [braid.core.client.gateway.views :refer [gateway-view]]
   [braid.core.client.gateway.forms.user-auth.views :refer [user-auth-view]]
   [braid.core.client.group-admin.views.group-settings-page :refer [group-settings-page-view]]
   [braid.core.client.invites.views.invite-page :refer [invite-page-view]]
   [braid.core.client.routes :as routes]
   [braid.core.client.ui.views.error-banner :refer [error-banner-view]]
   [braid.core.client.ui.views.header :refer [header-view readonly-header-view]]
   [braid.core.client.ui.views.pages.changelog :refer [changelog-view]]
   [braid.core.client.ui.views.pages.global-settings :refer [global-settings-page-view]]
   [braid.core.client.ui.views.pages.group-explore :refer [group-explore-page-view]]
   [braid.core.client.ui.views.pages.inbox :refer [inbox-page-view]]
   [braid.core.client.ui.views.pages.me :refer [me-page-view]]
   [braid.core.client.ui.views.pages.recent :refer [recent-page-view]]
   [braid.core.client.ui.views.pages.search :refer [search-page-view]]
   [braid.core.client.ui.views.pages.tags :refer [tags-page-view]]
   [braid.core.client.ui.views.pages.thread :refer [single-thread-view]]
   [braid.core.client.ui.views.reconnect-overlay :refer [reconnect-overlay-view]]
   [braid.core.client.ui.views.sidebar :refer [sidebar-view]]
   [braid.core.client.uploads.views.uploads-page :refer [uploads-page-view]]
   [braid.core.client.ui.views.pages.readonly :refer [readonly-inbox-page-view]]
   [re-frame.core :refer [subscribe]]))

(defn page-view []
  (case (@(subscribe [:page]) :type)
    :inbox [inbox-page-view]
    :readonly [readonly-inbox-page-view]
    :recent [recent-page-view]
    :search [search-page-view]
    :tags [tags-page-view]
    :me [me-page-view]
    :invite [invite-page-view]
    :group-explore [group-explore-page-view]
    :bots [bots-page-view]
    :uploads [uploads-page-view]
    :thread [single-thread-view]
    :settings [group-settings-page-view]
    :global-settings [global-settings-page-view]
    :changelog [changelog-view]
    nil))

(defn readonly-page-view
  []
  (case (:type @(subscribe [:page]))
    :readonly [readonly-inbox-page-view]
    :login [gateway-view]
    nil))

(defn main-view []
  (case @(subscribe [:login-state])
    :gateway
    [:div.main
     [sidebar-view]
     [:div.gateway
      [user-auth-view]]]

    :anon-connected
    [:div.main
     [sidebar-view]
     (when @(subscribe [:active-group])
       [readonly-header-view])
     [readonly-page-view]]

    [:div.main
     [error-banner-view]
     [reconnect-overlay-view]
     [sidebar-view]
     (when @(subscribe [:open-group-id])
       [header-view])
     [page-view]]))
