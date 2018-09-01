(ns braid.core.core
  "Braid core"
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[re-frame.core :refer [dispatch]]
          [braid.core.client.ui.views.autocomplete]
          [braid.core.client.store]
          [braid.popovers.api :as popovers]
          [braid.core.client.ui.styles.hover-menu]
          [braid.core.client.ui.styles.hover-cards]
          [braid.core.client.bots.views.bots-page :refer [bots-page-view]]
          [braid.core.client.ui.views.pages.recent :refer [recent-page-view]]
          [braid.core.client.ui.views.pages.search :refer [search-page-view]]
          [braid.core.client.group-admin.views.group-settings-page :refer [group-settings-page-view]]])))

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
          :view bots-page-view})

       (core/register-group-page!
         {:key :recent
          :view recent-page-view
          :on-load (fn [page]
                     (dispatch [:set-page-loading true])
                     (dispatch [:load-recent-threads
                                {:group-id (page :group-id)
                                 :on-complete (fn [_]
                                                (dispatch [:set-page-loading false]))
                                 :on-error (fn [e]
                                             (dispatch [:set-page-loading false])
                                             (dispatch [:set-page-error true]))}]))})

       (core/register-group-page!
         {:key :settings
          :view group-settings-page-view})

       (core/register-group-page!
         {:key :search
          :view search-page-view
          :on-load (fn [page]
                     (dispatch [:set-page-loading true])
                     (dispatch [:search-history [(page :query) (page :group-id)]]))}))))

