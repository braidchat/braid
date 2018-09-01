(ns braid.core.core
  "Braid core"
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[braid.core.client.ui.views.autocomplete]
          [braid.core.client.store]
          [braid.popovers.api :as popovers]
          [braid.core.client.ui.styles.hover-menu]
          [braid.core.client.ui.styles.hover-cards]
          [braid.core.client.bots.views.bots-page :refer [bots-page-view]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-autocomplete-engine!
         braid.core.client.ui.views.autocomplete/bot-autocomplete-engine)

       (core/register-autocomplete-engine!
         braid.core.client.ui.views.autocomplete/user-autocomplete-engine)

       (core/register-autocomplete-engine!
         braid.core.client.ui.views.autocomplete/tag-autocomplete-engine)

       (core/register-state! braid.core.client.store/initial-state
                             braid.core.client.store/AppState)

       (popovers/register-popover-styles!
         braid.core.client.ui.styles.hover-cards/>user-card)

       (popovers/register-popover-styles!
         braid.core.client.ui.styles.hover-cards/>tag-card)

       (popovers/register-popover-styles!
         (braid.core.client.ui.styles.hover-menu/>hover-menu))

       (core/register-group-page!
         {:key :bots
          :view bots-page-view}))))
