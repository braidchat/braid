(ns braid.ui.views.error-banner
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.store :as store]))

(defn error-banner-view []
  (let [errors (subscribe [:errors])]
    (fn []
      [:div.error-banners
       (doall
         (for [[err-key err cls] @errors]
           ^{:key err-key}
           [:div.error-banner
            {:class cls}
            err
            [:span.close
             {:on-click (fn [_] (store/clear-error! err-key))}
             "×"]]))])))
