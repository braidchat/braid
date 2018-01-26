(ns braid.client.ui.views.styles
  (:require
   [garden.core :refer [css]]
   [schema.core :as s]
   [braid.core.api :as api]
   [braid.client.ui.styles.animations]
   [braid.client.ui.styles.embed]
   [braid.client.ui.styles.body]
   [braid.client.ui.styles.header]
   [braid.client.ui.styles.imports]
   [braid.client.ui.styles.message]
   [braid.client.ui.styles.misc]
   [braid.client.ui.styles.page]
   [braid.client.ui.styles.pages.tags]
   [braid.client.ui.styles.pages.me]
   [braid.client.ui.styles.pages.search]
   [braid.client.ui.styles.pills]
   [braid.client.ui.styles.reconnect-overlay]
   [braid.client.ui.styles.sidebar]
   [braid.client.ui.styles.thread]
   [braid.client.ui.styles.vars :as vars]
   [braid.client.bots.views.bots-page-styles]
   [braid.client.group-admin.views.group-settings-page-styles]
   [braid.client.invites.views.invite-page-styles]
   [braid.client.uploads.views.uploads-page-styles]
   [reagent.core :as r]))

(def styles (r/atom []))

(defn ^:api register-styles!
  [more-styles]
  (swap! styles conj more-styles))

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
               braid.client.ui.styles.animations/anim-spin
               braid.client.ui.styles.body/body
               braid.client.ui.styles.imports/imports
               (braid.client.ui.styles.misc/avatar-upload vars/pad)
               braid.client.ui.styles.misc/page-headers
               (braid.client.ui.styles.pills/tag)
               (braid.client.ui.styles.pills/user)

               (doall (mapv #(%) @styles))

               [:#app
                braid.client.ui.styles.misc/status]

               [:.app

                [:>.main
                 (braid.client.ui.styles.header/header vars/pad)

                 braid.client.ui.styles.sidebar/sidebar
                 braid.client.ui.styles.misc/error-banners
                 braid.client.ui.styles.reconnect-overlay/reconnect-overlay
                 braid.client.ui.styles.page/page

                 ; page styles
                 braid.client.ui.styles.pages.tags/tags-page
                 braid.client.ui.styles.pages.me/me-page
                 braid.client.group-admin.views.group-settings-page-styles/group-settings-page
                 braid.client.uploads.views.uploads-page-styles/uploads-page
                 braid.client.invites.views.invite-page-styles/invite-page
                 braid.client.bots.views.bots-page-styles/bots-page
                 braid.client.ui.styles.pages.search/search-user-card

                 [:>.page
                  (braid.client.ui.styles.misc/threads vars/pad)

                  [:&.single-thread
                   [:.content
                    (braid.client.ui.styles.thread/thread vars/pad)
                    (braid.client.ui.styles.thread/notice vars/pad)
                    [:>.thread

                     [:>.card

                      [:>.messages
                       braid.client.ui.styles.message/message

                       [:>.message
                        (braid.client.ui.styles.embed/embed vars/pad)]]

                      (braid.client.ui.styles.thread/new-message vars/pad)]]]]


                  [:>.threads
                   (braid.client.ui.styles.thread/thread vars/pad)
                   (braid.client.ui.styles.thread/notice vars/pad)

                   [:>.thread

                    [:>.card

                     [:>.messages
                      braid.client.ui.styles.message/message

                      [:>.message
                       (braid.client.ui.styles.embed/embed vars/pad)]]

                     (braid.client.ui.styles.thread/new-message vars/pad)]]]]]])}}])
