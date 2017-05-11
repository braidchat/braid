(ns braid.client.gateway.log-in.views
  (:require
    [re-frame.core :refer [subscribe]]))

(defn log-in-view []
  (let [logged-in? @(subscribe [:gateway.user-auth/user])]
    (when logged-in?
      [:div {:ref (fn []
                    (set! js/window.location "/"))}])))
