(ns braid.disconnect-notice.core
  (:require
   [braid.base.api :as base]
   #?@(:cljs
       [[braid.disconnect-notice.ui :as ui]
        [braid.disconnect-notice.styles :as styles]])))

(defn init! []
  #?(:cljs
     (do
       (base/register-root-view! ui/disconnect-notice-view)
       (base/register-styles! [:#app>.app>.main styles/disconnect-notice]))))
