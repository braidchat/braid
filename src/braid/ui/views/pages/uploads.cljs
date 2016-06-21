(ns braid.ui.views.pages.uploads
  (:require [reagent.core :as r]
            [reagent.ratom :as ra :refer-macros [reaction]]
            [clojure.string :as string]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.s3 :as s3]
            [braid.ui.views.embed :refer [embed-view]]
            [chat.client.views.helpers :as helpers]))

(defn uploads-view
  []
  (let [group-id (subscribe [:open-group-id])
        uploads (r/atom nil)
        dummy (ra/run! (s3/uploads-in-group
                         @group-id
                         (fn [res] (reset! uploads res))))]
    (fn []
      (let [_ @dummy]
        [:div.page.uploads
         [:div.title "Uploads"]
         [:div.content
          [:h2 "Uploaded files"]
          (if-let [fs @uploads]
            (into
              [:div.files]
              (for [f fs]
                ^{:key (:ETag f)}
                [:div.file
                 [:a.external
                  {:href (:Key f)
                   :style {:background-color (helpers/->color (:Key f))}}
                  (last (string/split (:Key f) #"/"))]
                 [embed-view (:Key f)]]))
            [:div.spinner])]]))))
