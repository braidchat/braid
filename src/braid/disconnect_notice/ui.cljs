(ns braid.disconnect-notice.ui
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]))

(defn disconnect-notice-view []
  (let [websocket-state (subscribe [:core/websocket-state])
        seconds-to-reconnect (r/atom nil)]
    (fn []
      (let [tick! (fn []
                    (reset! seconds-to-reconnect
                            (js/Math.round
                              (/ (- (@websocket-state :next-reconnect)
                                    (.valueOf (js/Date.)))
                                 1000))))]
        (when-not (@websocket-state :connected?)
          [:div.disconnect-notice
           {:ref (fn [_]
                   (tick!)
                   (js/setTimeout (fn [_]
                                    (tick!))
                                  1000))}
           [:div.info
            [:h1 "Uh oh! Braid has disconnected from the server."]
            [:div.message "Attempting to reconnect in " @seconds-to-reconnect " seconds... "
             [:button
              {:on-click (fn [_]
                           (dispatch [:core/reconnect!]))}
              "Reconnect Now"]]]])))))
