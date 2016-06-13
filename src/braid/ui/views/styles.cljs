(ns braid.ui.views.styles
  (:require [garden.core :refer [css]]
            [braid.ui.styles.message]
            [braid.ui.styles.thread]
            [braid.ui.styles.header]
            [braid.ui.styles.body]
            [braid.ui.styles.sidebar]
            [braid.ui.styles.imports]
            [braid.ui.styles.misc]
            [braid.ui.styles.animations]
            [braid.ui.styles.embed]
            [braid.ui.styles.login]
            [braid.ui.styles.vars :as vars]))

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
               braid.ui.styles.imports/imports
               braid.ui.styles.animations/anim-spin
               braid.ui.styles.body/body
               braid.ui.styles.message/message
               braid.ui.styles.misc/layout
               braid.ui.styles.sidebar/sidebar
               braid.ui.styles.misc/emojione
               braid.ui.styles.misc/error-banners
               braid.ui.styles.misc/page
               braid.ui.styles.misc/page-headers
               braid.ui.styles.misc/channels-page
               braid.ui.styles.misc/settings-page
               braid.ui.styles.misc/me-page
               braid.ui.styles.misc/bots-page
               braid.ui.styles.login/login
               braid.ui.styles.misc/tag
               braid.ui.styles.misc/user
               braid.ui.styles.misc/button
               braid.ui.styles.misc/status
               (braid.ui.styles.misc/threads vars/pad)
               (braid.ui.styles.thread/thread vars/pad)
               (braid.ui.styles.thread/head vars/pad)
               (braid.ui.styles.thread/messages vars/pad)
               (braid.ui.styles.thread/new-message vars/pad)
               (braid.ui.styles.thread/notice vars/pad)
               (braid.ui.styles.thread/drag-and-drop vars/pad)
               (braid.ui.styles.header/header vars/pad)
               (braid.ui.styles.embed/embed vars/pad)
               )}}])
