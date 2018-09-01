(ns braid.permalinks.core
  "Includes a permalink in thread headers that leads to a dedicated page"
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[re-frame.core :refer [subscribe dispatch]]
          [braid.core.client.routes :as routes]
          [braid.core.client.ui.views.threads :refer [threads-view]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-thread-control!
         {:priority -1
          :view
          (fn [thread]
            [:a.control.permalink
             {:title "Go to Permalink"
              :href (routes/group-page-path
                      {:query-params {:thread-id (thread :id)}
                       :page-id "thread"
                       :group-id (thread :group-id)})}
             \uf0c1])})

       (core/register-group-page!
         {:key :thread
          :on-load (fn [page]
                     (dispatch [:load-threads {:thread-ids [(uuid (page :thread-id))]}]))
          :view
          (fn []
            [:div.page
             [:div.intro
              [:div.title "Thread"]]
             (let [page @(subscribe [:page])]
               (if-let [thread @(subscribe [:thread (uuid (page :thread-id))])]
                 [threads-view {:threads [thread]}]
                 [:div.loading]))])}))))

