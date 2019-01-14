(ns braid.disconnect-notice.core
  (:require
   [braid.core.api :as core]
   #?@(:cljs
       [[braid.disconnect-notice.ui :as ui]
        [braid.disconnect-notice.styles :as styles]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-root-view! ui/disconnect-notice-view)
       (core/register-styles! [:#app>.app>.main styles/disconnect-notice]))))

