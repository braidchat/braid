(ns braid.uploads.core
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[cljs-uuid-utils.core :as uuid]
          [re-frame.core :refer [subscribe dispatch]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-events!
         {:braid.uploads/create-upload
          (fn [{state :db} [_ {:keys [url thread-id group-id]}]]
            {:websocket-send (list [:braid.server/create-upload
                                    {:id (uuid/make-random-squuid)
                                     :url url
                                     :thread-id thread-id
                                     :group-id group-id}])
             :dispatch [:new-message {:content url
                                      :thread-id thread-id
                                      :group-id group-id}]})})
       (core/register-new-message-action-menu-item!
         {:body "upload"
          :priority 1}))
     :clj
     (do)))

#_[:label.plus {:class (when @uploading? "uploading")}
[:input {:type "file"
           :multiple false
           :style {:display "none"}
           :on-change (fn [e]
                        (reset! uploading? true)
                        (s3/upload
                          (aget (.. e -target -files) 0)
                          (fn [url]
                            (reset! uploading? false)
                            (dispatch [:braid.uploads/create-upload
                                       {:url url
                                        :group-id (config :group-id)
                                        :thread-id (config :thread-id)}]))))}]]
