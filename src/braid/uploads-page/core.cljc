(ns braid.uploads-page.core
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[cljs-uuid-utils.core :as uuid]
          [re-frame.core :refer [subscribe dispatch]]
          [braid.core.client.routes :as routes]
          [braid.uploads-page.views.uploads-page-styles :refer [>uploads-page]]
          [braid.uploads-page.views.uploads-page :refer [uploads-page-view]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-group-page!
         {:key :uploads
          :on-load (fn [_]
                     ;; TODO: will need to page this when it gets big?
                     (dispatch [:braid.uploads-page/get-group-uploads!]))
          :view uploads-page-view})

       (core/register-styles!
         [:#app>.app>.main
          (>uploads-page)])

       (core/register-group-header-button!
         {:title "Uploads"
          :class "uploads"
          :icon \uf0ee
          :priority 0
          :route-fn routes/page-path
          :route-args {:page-id "uploads"}})

       (core/register-subs!
         {:braid.uploads-page/uploads
          (fn [db _]
            (get-in db [:uploads (db :open-group-id)]))

          :braid.uploads-page/error
          (fn [db _]
            nil)})

       (core/register-events!
         {::store-group-uploads!
          (fn [{db :db} [_ group-id uploads]]
            {:db (assoc-in db [:uploads group-id] uploads)})

          :braid.uploads-page/get-group-uploads!
          (fn [{state :db} _]
            (let [group-id (state :open-group-id)]
              {:websocket-send
               (list
                 [:braid.server/uploads-in-group group-id]
                 5000
                 (fn [reply]
                   (if-let [uploads (:braid/ok reply)]
                     (dispatch [::store-group-uploads! group-id uploads])
                     ;; TODO handle error
                     (do
                       #_(on-error (get reply :braid/error "Couldn't get uploads in group"))))))}))

          :braid.uploads-page/delete-upload
          (fn [{db :db} [_ group-id upload-id]]
            {:websocket-send (list [:braid.server/delete-upload upload-id])
             :db (update-in db [:uploads group-id]
                            (fn [uploads]
                              (remove
                                (fn [upload]
                                  (= (upload :id) upload-id))
                                uploads)))})}))))

