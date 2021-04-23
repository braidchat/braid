(ns braid.page-uploads.core
  (:require
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    #?@(:cljs
         [[cljs-uuid-utils.core :as uuid]
          [re-frame.core :refer [subscribe dispatch]]
          [spec-tools.data-spec :as ds]
          [braid.core.client.routes :as routes]
          [braid.page-uploads.views.uploads-page-styles :refer [>uploads-page]]
          [braid.page-uploads.views.uploads-page :refer [uploads-page-view]]])))

(defn init! []
  #?(:cljs
     (do
       (chat/register-group-page!
         {:key :uploads
          :on-load (fn [_]
                     ;; TODO: will need to page this when it gets big?
                     (dispatch [:braid.uploads-page/get-group-uploads!]))
          :view uploads-page-view
          :styles (>uploads-page)})

       (chat/register-group-header-button!
         {:title "Uploads"
          :class "uploads"
          :icon \uf093
          :priority 0
          :route-fn routes/group-page-path
          :route-args {:page-id "uploads"}})

       (base/register-state!
         {::uploads {}}
         {::uploads {uuid? (ds/maybe [{:id uuid?
                                       :url string?
                                       :thread-id uuid?}])}})

       (base/register-subs!
         {:braid.uploads-page/uploads
          (fn [db _]
            (get-in db [::uploads (db :open-group-id)]))})

       (base/register-events!
         {::store-group-uploads!
          (fn [{db :db} [_ group-id uploads]]
            {:db (assoc-in db [::uploads group-id] uploads)})

          :braid.uploads-page/get-group-uploads!
          (fn [{state :db} _]
            (when-let [group-id (state :open-group-id)]
              {:websocket-send
               (list
                 [:braid.server/uploads-in-group group-id]
                 5000
                 (fn [reply]
                   (if-let [uploads (:braid/ok reply)]
                     (dispatch [::store-group-uploads! group-id uploads])
                     (dispatch [:braid.notices/display! [::fetching-uploads "Error loading uploads." :error]]))))}))

          :braid.uploads-page/delete-upload
          (fn [{db :db} [_ group-id upload-id]]
            {:websocket-send (list [:braid.server/delete-upload upload-id])
             :db (update-in db [::uploads group-id]
                            (fn [uploads]
                              (remove
                                (fn [upload]
                                  (= (upload :id) upload-id))
                                uploads)))})}))))

