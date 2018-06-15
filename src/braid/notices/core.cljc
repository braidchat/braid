(ns braid.notices.core
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[clojure.spec.alpha :as s]
          [re-frame.core :refer [subscribe dispatch]]
          [garden.units :refer [rem em]]])))

#?(:cljs
   (do
     (def ErrorSpec
       {:key keyword?
        :message string?
        :class (s/spec #{:error :warn :info})})))

(defn init! []
  #?(:cljs
     (do
       #_(core/register-root-view!
           (fn []
             [:div {:style {:z-index 99000
                            :position "absolute"}}
              [:button {:on-click (fn []
                                    (dispatch [:display-error [:error "Error" :error]]))}
               "ERROR"]
              [:button {:on-click (fn []
                                    (dispatch [:display-error [(gensym :error) "Error" :error]]))}
               "ERROR+"]
              [:button {:on-click (fn []
                                    (dispatch [:display-error [:notice "Notice" :info]]))}
               "NOTICE"]]))

       (core/register-state!
         {::state {}}
         {::state {keyword? ErrorSpec}})

       (core/register-subs!
         {::sub
          (fn [state _]
            (vals (state ::state)))})

       (core/register-events!
         {:braid.notices/remove-message-errors!
          (fn [{db :db} [_ msg-ids]]
            {:db (update db ::state (fn [errors]
                                      (apply dissoc errors msg-ids)))})

          :clear-error
          (fn [{db :db} [_ err-key]]
            {:db (update db ::state dissoc err-key)})

          :display-error
          (fn [{db :db} [_ [err-key message error-class]]]
            {:db (update db ::state assoc err-key {:key err-key
                                                   :message message
                                                   :class error-class})})})

       (core/register-root-view!
         (fn []
           [:div.error-banners
            (doall
              (for [{:keys [key message class]} @(subscribe [::sub])]
                ^{:key key}
                [:div.error-banner
                 {:class class}
                 message
                 [:span.close
                  {:on-click (fn [_] (dispatch [:clear-error key]))}
                  "×"]]))]))

       (core/register-styles!
         [:#app>.app>.main
          [:>.error-banners
           {:z-index 9999
            :position "fixed"
            :top 0
            :right 0
            :width "100%"}

           [:>.error-banner
            {:margin-bottom (rem 0.25)
             :font-size (em 2)
             :text-align "center"}

            [:&.error
             {:background-color "rgba(255, 5, 14, 0.6)"}]

            [:&.warn
             {:background-color "rgba(255, 190, 5, 0.6)"}]

            [:&.info
             {:background-color "rgba(5, 255, 70, 0.6)"}]

            [:>.close
             {:margin-left (em 1)
              :cursor "pointer"}]]]]))))
