(ns braid.notices.core
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[clojure.spec.alpha :as s]
          [garden.units :refer [rem em]]
          [re-frame.core :refer [subscribe dispatch]]
          [braid.core.client.ui.styles.mixins :as mixins]])))

#?(:cljs
   (do
     (def ErrorSpec
       {:key keyword?
        :message string?
        :class (s/spec #{:error :info})})))

(defn init! []
  #?(:cljs
     (do


       (core/register-state!
         {::state {}}
         {::state {keyword? ErrorSpec}})

       (core/register-subs!
         {::sub
          (fn [state _]
            (vals (state ::state)))})

       (core/register-events!
         {:braid.notices/clear!
          (fn [{db :db} [_ korks]]
            {:db (update db ::state (fn [errors]
                                      (if (sequential? korks)
                                        (apply dissoc errors korks)
                                        (dissoc errors korks))))})

          :braid.notices/display!
          (fn [{db :db} [_ [err-key message error-class]]]
            {:db (update db ::state assoc err-key {:key err-key
                                                   :message message
                                                   :class error-class})})})

       (core/register-root-view!
         (fn []
           [:div.notices
            (doall
              (for [{:keys [key message class]} @(subscribe [::sub])]
                ^{:key key}
                [:div.notice
                 {:class class}
                 message
                 [:span.gap]
                 [:span.close
                  {:on-click (fn [_] (dispatch [:braid.notices/clear! key]))}
                  "×"]]))]))

       (core/register-styles!
         [:#app>.app>.main
          [:>.notices
           {:z-index 9999
            :position "fixed"
            :top "4rem"
            :right "1rem"}

           [:>.notice
            {:display "flex"
             :justify-content "space-between"
             :min-width "10em"
             :align-items "center"
             :margin [[0 "auto" (rem 0.25)]]
             :font-size (em 1.5)
             :padding "0.25em"
             :border-radius "3px"
             :border [["0.5px" "solid" "#000000"]]}

            [:&.error
             {:background "#ffe4e4"
              :border-color "#CA1414"
              :color "#CA1414"}

             [:&:before
              (mixins/fontawesome \uf071)
              {:margin "0 0.5rem 0 0.25rem"}]]

            [:&.info
             {:background "#cfe9ff"
              :border-color "#236cab"
              :color "#236cab"}

             [:&:before
              (mixins/fontawesome \uf06a)
              {:margin "0 0.5rem 0 0.25rem"}]]

            [:>.gap
             {:flex-grow 1}]

            [:>.close
             {:cursor "pointer"
              :margin "0 0.25rem"}]]]]))))
