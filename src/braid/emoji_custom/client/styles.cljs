(ns braid.emoji-custom.client.styles
  (:require
    [braid.core.client.ui.styles.mixins :as mixins]))

(def settings-page
  [:.app>.main>.page.group-settings>.content
   [:.settings.custom-emoji
    [:button.delete
     {:color "red"}
     (mixins/fontawesome nil)]]])
