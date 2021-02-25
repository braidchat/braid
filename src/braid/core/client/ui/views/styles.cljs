(ns braid.core.client.ui.views.styles
  (:require
   [braid.core.client.gateway.forms.user-auth.styles]
   [braid.core.client.gateway.styles-vars :as gateway-vars]
   [braid.core.client.gateway.styles]
   [braid.core.client.group-admin.views.group-settings-page-styles]
   [braid.core.client.invites.views.invite-page-styles]
   [braid.core.client.ui.styles.animations]
   [braid.core.client.ui.styles.body]
   [braid.core.client.ui.styles.header]
   [braid.core.client.ui.styles.imports]
   [braid.core.client.ui.styles.misc]
   [braid.core.client.ui.styles.page]
   [braid.core.client.ui.styles.pages.global-settings]
   [braid.core.client.ui.styles.pages.me]
   [braid.core.client.ui.styles.pills]
   [braid.core.client.ui.styles.threads]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.core :refer [css]]
   [reagent.core :as r]))

;; don't add to here
;; use base.api/register-styles! instead
;; the following is still around from the 'pre-module' days
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

               [:#app
                braid.core.client.ui.styles.misc/status]

               [:.app

                [:>.main
                 (braid.core.client.ui.styles.header/header vars/pad)

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
                 (braid.core.client.gateway.forms.user-auth.styles/user-auth-styles)

                 ;; page styles

                 braid.core.client.ui.styles.pages.me/me-page
                 braid.core.client.group-admin.views.group-settings-page-styles/group-settings-page
                 braid.core.client.invites.views.invite-page-styles/invite-page
                 braid.core.client.ui.styles.pages.global-settings/global-settings-page

                 [:>.page
                  (braid.core.client.ui.styles.threads/>threads)]]])}}])
