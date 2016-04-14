(ns braid.ui.views.error-banner
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.store :as store]))

(defn error-banner-view []
  (let [errors (subscribe [:errors])]
    (fn []
      [:div.error-banners
       (for [[err-key err] @errors]
         [:div.error-banner
          err
          [:span.close
           {:on-click (fn [_] (store/clear-error! err-key))}
           "×"]])])))
