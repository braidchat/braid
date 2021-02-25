(ns braid.core.client.ui.views.main
  (:require
   [braid.core.client.gateway.views :refer [gateway-view]]
   [braid.core.client.gateway.forms.user-auth.views :refer [user-auth-view]]
   [braid.base.client.pages :as pages]
   [braid.base.client.root-view :as root-view]
   [braid.core.client.routes :as routes]
   [braid.core.client.ui.views.header :refer [header-view readonly-header-view]]
   [braid.core.client.ui.views.pages.readonly :refer [readonly-inbox-page-view]]
   [re-frame.core :refer [dispatch subscribe]]))

(defn page-view []
  (let [page @(subscribe [:page])
        id (:type page)]
    (case id
      :readonly [readonly-inbox-page-view]
      :login [gateway-view]
      (when-let [view (pages/get-view id)]
        [view page]))))

(defn readonly-page-view
  []
  (let [page @(subscribe [:page])
        id (:type page)]
    (case id
      (:inbox :readonly) [readonly-inbox-page-view]
      :login [gateway-view]
      (when-let [view (pages/get-view id)]
        [view page]))))

(defn root-views []
  (into
    [:<>]
    (for [view @root-view/root-views]
      [view])))

(defn main-view []
  (case @(subscribe [:login-state])
    :gateway
    [:div.main
     [:div.gateway
      [user-auth-view]]]

    :anon-connected
    [:div.main
     ;; TODO [sidebar-view]
     (when @(subscribe [:active-group])
       [readonly-header-view])
     [readonly-page-view]
     [root-views]]

    ;; default
    (into
      [:div.main
       (when @(subscribe [:open-group-id])
         [header-view])
       [page-view]
       [root-views]])))
