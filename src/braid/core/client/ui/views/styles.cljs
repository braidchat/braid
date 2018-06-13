(ns braid.core.client.ui.views.styles
  (:require
   [braid.core.hooks :as hooks]
   [braid.core.client.bots.views.bots-page-styles]
   [braid.core.client.gateway.forms.create-group.styles]
   [braid.core.client.gateway.forms.user-auth.styles]
   [braid.core.client.gateway.styles-vars :as gateway-vars]
   [braid.core.client.gateway.styles]
   [braid.core.client.group-admin.views.group-settings-page-styles]
   [braid.core.client.invites.views.invite-page-styles]
   [braid.core.client.ui.styles.animations]
   [braid.core.client.ui.styles.body]
   [braid.core.client.ui.styles.header]
   [braid.core.client.ui.styles.imports]
   [braid.core.client.ui.styles.message]
   [braid.core.client.ui.styles.misc]
   [braid.core.client.ui.styles.page]
   [braid.core.client.ui.styles.pages.group-explore]
   [braid.core.client.ui.styles.pages.me]
   [braid.core.client.ui.styles.pages.search]
   [braid.core.client.ui.styles.pages.tags]
   [braid.core.client.ui.styles.pills]
   [braid.core.client.ui.styles.reconnect-overlay]
   [braid.core.client.ui.styles.sidebar]
   [braid.core.client.ui.styles.thread]
   [braid.core.client.ui.styles.vars :as vars]
   [braid.core.client.uploads.views.uploads-page-styles]
   [garden.core :refer [css]]
   [reagent.core :as r]))

(def style-dataspec
  #(or (vector? %) (map? %)))

(defonce module-styles
  (hooks/register!
    (atom [] [style-dataspec])))

(defn styles-view []
  [:style
   {:type "text/css"
    :dangerouslySetInnerHTML
    {:__html (css
               {:auto-prefix #{:transition
                               :flex-direction
                               :flex-shrink
                               :align-items
                               :animation
                               :flex-grow}
                :vendors ["webkit"]}
               braid.core.client.ui.styles.animations/anim-spin
               braid.core.client.ui.styles.body/body
               braid.core.client.ui.styles.imports/imports
               (braid.core.client.ui.styles.misc/avatar-upload vars/pad)
               braid.core.client.ui.styles.misc/page-headers
               (braid.core.client.ui.styles.pills/tag)
               (braid.core.client.ui.styles.pills/user)

               @module-styles

               [:#app
                braid.core.client.ui.styles.misc/status]

               [:.app

                [:>.main
                 (braid.core.client.ui.styles.header/header vars/pad)

                 braid.core.client.ui.styles.sidebar/sidebar
                 braid.core.client.ui.styles.misc/error-banners
                 braid.core.client.ui.styles.reconnect-overlay/reconnect-overlay
                 braid.core.client.ui.styles.page/page

                 [:.gateway
                  {:min-height "100vh"
                   :font-family gateway-vars/font-family
                   :margin 0
                   :line-height 1.5
                   :background gateway-vars/page-background-color
                   :color gateway-vars/primary-text-color}

                  [:input
                   :button
                   {:font-family gateway-vars/font-family}]]
                 (braid.core.client.gateway.styles/form-styles)
                 [:.page.group-explore>.content
                  (braid.core.client.gateway.forms.create-group.styles/create-group-styles)]
                 (braid.core.client.gateway.forms.user-auth.styles/user-auth-styles)

                 ;; page styles
                 braid.core.client.ui.styles.pages.tags/tags-page
                 braid.core.client.ui.styles.pages.me/me-page
                 braid.core.client.ui.styles.pages.group-explore/group-explore-page
                 braid.core.client.group-admin.views.group-settings-page-styles/group-settings-page
                 braid.core.client.uploads.views.uploads-page-styles/uploads-page
                 braid.core.client.invites.views.invite-page-styles/invite-page
                 braid.core.client.bots.views.bots-page-styles/bots-page
                 braid.core.client.ui.styles.pages.search/search-user-card

                 [:>.page
                  (braid.core.client.ui.styles.misc/threads vars/pad)

                  [:>.threads
                   (braid.core.client.ui.styles.thread/thread vars/pad)
                   (braid.core.client.ui.styles.thread/notice vars/pad)

                   [:>.thread

                    [:>.card

                     [:>.messages
                      braid.core.client.ui.styles.message/message]

                     (braid.core.client.ui.styles.thread/new-message vars/pad)]]]]]])}}])
