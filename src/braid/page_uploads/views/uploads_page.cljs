(ns braid.page-uploads.views.uploads-page
  (:require
   [clojure.string :as string]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [braid.lib.date :as date]
   [braid.lib.upload :as upload]
   [braid.core.client.routes :as routes]
   [braid.core.client.ui.views.mentions :as mentions]
   [braid.core.client.ui.views.thread-header :as thread-header]))

(defn file-view [url]
  (cond
    (re-matches #".*(png|jpg|jpeg|gif)" url)
    [:img {:src (upload/->path url)}]
    (re-matches #".*(mp4|mkv|mov)$" url)
    [:video
     {:src (upload/->path url)
      :preload "metadata"
      :controls true}]))

(defn upload-view
  [upload]
  (let [group-id (subscribe [:open-group-id])
        thread-id (r/atom (upload :thread-id))
        user-id (subscribe [:user-id])
        thread (subscribe [:thread*] [thread-id])]
    (r/create-class
      {:display-name "upload-view"

       :component-will-update
       (fn [_ [_ new-upload]]
         (reset! thread-id (new-upload :thread-id)))

       :reagent-render
       (fn [upload]
         [:tr.upload
          [:td.uploaded-file
           [file-view (upload :url)]
           [:br]
           (js/decodeURIComponent (last (string/split (upload :url) #"/")))]
          [:td.delete
           (when (or (= @(subscribe [:user-id]) (upload :uploader-id))
                     @(subscribe [:current-user-is-group-admin?] [group-id]))
             [:button
              {:on-click
               (fn [_]
                 (when (js/confirm (str "Delete this uploaded file?\n"
                                        "This will also remove it from S3."))
                   (dispatch [:braid.uploads-page/delete-upload @group-id (upload :id)])))}
              \uf1f8])]
          [:td.uploader
           "Uploaded by "
           [mentions/user-mention-view (upload :uploader-id)]]
          [:td.uploaded-thread
           [:a {:href (routes/group-page-path
                        {:group-id @group-id
                         :page-id "thread"
                         :query-params {:thread-id (upload :thread-id)}})}
            (str "Uploaded at " (date/smart-format-date (upload :uploaded-at)))]
           [:br]
           (if @thread
             [:span "Tagged with " [thread-header/thread-tags-view @thread]]
             [:button
              {:on-click (fn [_]
                           (dispatch [:load-threads!
                                      {:thread-ids [(upload :thread-id)]}]))}
              "Load thread to see tags"])]])})))

(defn uploads-page-view
  []
  (let [uploads @(subscribe [:braid.uploads-page/uploads])]
    [:div.page.uploads
     [:div.title "Uploads"]
     [:div.content
      (cond
        (nil? uploads)
        [:p "Loading..."]

        (empty? uploads)
        [:p "No uploads in this group yet"]

        :else
        [:table.uploads
         [:tbody
          (doall
            (for [upload uploads]
              ^{:key (upload :id)}
              [upload-view upload]))]])]]))
