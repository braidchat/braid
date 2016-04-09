(ns braid.ui.views.pills
  (:require [om.core :as om]
            [chat.client.routes :as routes]
            [chat.client.views.helpers :refer [id->color user-cursor]]))

(defn tag-pill-view
  [tag subscribe]
  (let [path (routes/tag-page-path {:group-id (routes/current-group)
                                    :tag-id (tag :id)})
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag (tag :id)])]
    (fn []
      [:a.tag.pill {:className (if @user-subscribed-to-tag?
                              " on"
                              " off")
                    :tabIndex -1
                    :style {:backgroundColor (id->color (tag :id))
                            :color (id->color (tag :id))
                            :borderColor (id->color (tag :id))}
                    :href path}
        [:div.name "#" (tag :name)]])))


(defn user-pill-view
  [user subscribe]
  (let [user-status (subscribe [:user-status (user :id)])
        path (routes/user-page-path {:group-id (routes/current-group)
                                     :user-id (user :id)})]
    (fn []
      [:a.user.pill {:className (case @user-status
                                              :online " on"
                                                      " off")
                     :tabIndex -1
                     :style {:backgroundColor (id->color (user :id))
                             :color (id->color (user :id))
                             :borderColor (id->color (user :id))}
                     :href path}
        [:span.name (str "@" (user :nickname))]
        #_[:div {:className (str "status " ((fnil name "") (user :status)))}]]
        [:a.user.pill {:tabIndex -1
                       :href path}
          [:span.name (str "@" (user :nickname))]])))
