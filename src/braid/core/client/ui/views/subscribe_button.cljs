(ns braid.core.client.ui.views.subscribe-button
  (:require
    [re-frame.core :refer [dispatch subscribe]]))

(defn subscribe-button-view
  [tag-id]
  (let [user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])]
    (if @user-subscribed-to-tag?
      [:a.button {:on-click
                  (fn [_]
                    (dispatch [:unsubscribe-from-tag! tag-id]))}
       "Unsubscribe"]
      [:a.button {:on-click
                  (fn [_]
                    (dispatch [:subscribe-to-tag! {:tag-id tag-id}]))}
       "Subscribe"])))
