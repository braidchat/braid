(ns braid.core.client.ui.views.main
  (:require
   [braid.core.hooks :as hooks]
   [braid.core.client.bots.views.bots-page :refer [bots-page-view]]
   [braid.core.client.gateway.views :refer [gateway-view]]
   [braid.core.client.gateway.forms.user-auth.views :refer [user-auth-view]]
   [braid.core.client.group-admin.views.group-settings-page :refer [group-settings-page-view]]
   [braid.core.client.invites.views.invite-page :refer [invite-page-view]]
   [braid.core.client.pages :as pages]
   [braid.core.client.routes :as routes]
   [braid.core.client.ui.views.header :refer [header-view readonly-header-view]]
   [braid.core.client.ui.views.pages.changelog :refer [changelog-view]]
   [braid.core.client.ui.views.pages.global-settings :refer [global-settings-page-view]]
   [braid.core.client.ui.views.pages.create-group :refer [create-group-page-view]]
   [braid.core.client.ui.views.pages.inbox :refer [inbox-page-view]]
   [braid.core.client.ui.views.pages.me :refer [me-page-view]]
   [braid.core.client.ui.views.pages.recent :refer [recent-page-view]]
   [braid.core.client.ui.views.pages.search :refer [search-page-view]]
   [braid.core.client.ui.views.pages.tags :refer [tags-page-view]]
   [braid.core.client.ui.views.reconnect-overlay :refer [reconnect-overlay-view]]
   [braid.core.client.ui.views.sidebar :refer [sidebar-view]]
   [braid.core.client.ui.views.pages.readonly :refer [readonly-inbox-page-view]]
   [re-frame.core :refer [dispatch subscribe]]))

(defn page-view []
  (let [id (@(subscribe [:page]) :type)]
    (case id
      :inbox [inbox-page-view]
      :readonly [readonly-inbox-page-view]
      :login [gateway-view]
      :recent [recent-page-view]
      :search [search-page-view]
      :tags [tags-page-view]
      :me [me-page-view]
      :invite [invite-page-view]
      :create-group [create-group-page-view]
      :bots [bots-page-view]
      :settings [group-settings-page-view]
      :global-settings [global-settings-page-view]
      :changelog [changelog-view]
      (when-let [view (pages/get-view id)]
        [view]))))

(defn readonly-page-view
  []
  (let [id (:type @(subscribe [:page]))]
    (case id
      (:inbox :readonly) [readonly-inbox-page-view]
      :login [gateway-view]
      :create-group [create-group-page-view]
      (when-let [view (pages/get-view id)]
        [view]))))

(defonce root-views (hooks/register! (atom []) [fn?]))

(defn main-view []
  (case @(subscribe [:login-state])
    :gateway
    [:div.main
     [sidebar-view]
     (case @(subscribe [:page-path])
       "/pages/create-group"
       [create-group-page-view]

       [:div.gateway
        [user-auth-view]])]

    :anon-connected
    [:div.main
     [sidebar-view]
     (when @(subscribe [:active-group])
       [readonly-header-view])
     [readonly-page-view]]

    (into
      [:div.main]
      (concat [[reconnect-overlay-view]
               [sidebar-view]
               (when @(subscribe [:open-group-id])
                 [header-view])
               [page-view]]
              (for [view @root-views]
                [view])))))
