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
                                      :group-id group-id}]})}))
     :clj
     (do)))

