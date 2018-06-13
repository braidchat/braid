(ns braid.core.core
  "Braid core"
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[braid.core.client.ui.views.autocomplete]
          [braid.core.client.store]])))

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
                             braid.core.client.store/AppState))))
