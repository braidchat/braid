(ns braid.core.client.gateway.helper-views
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.ratom :refer-macros [run!]]))

(defn with-ns [ns n]
  (keyword (name ns) (name n)))

(defn field-view [opts]
  (let [field-id (opts :id)
        value @(subscribe [(with-ns (opts :subs-ns) :field-value) field-id])
        status @(subscribe [(with-ns (opts :subs-ns) :field-status) field-id])
        errors @(subscribe [(with-ns (opts :subs-ns) :field-errors) field-id])
        on-change-transform (or (opts :on-change-transform) identity)
        value-atom (r/atom value)]
    (fn [opts]
      (run! (reset! value-atom @(subscribe [(with-ns (opts :subs-ns) :field-value) field-id])))
      [:div.option
       {:class (str (opts :class) " " (name status))}
       [:h2 (opts :title)]
       [:label
        [:div.field
         (when (opts :pre-input)
           (opts :pre-input))
         [:input {:type (opts :type)
                  :placeholder (opts :placeholder)
                  :auto-complete (opts :auto-complete)
                  :auto-correct "off"
                  :auto-capitalize "off"
                  :spell-check "false"
                  :auto-focus (opts :auto-focus)
                  :value @value-atom
                  :on-key-down (or (opts :on-key-down) (fn [_]))
                  :on-blur (fn [e]
                             (when (opts :on-blur)
                               ((opts :on-blur) e))
                             (dispatch [(with-ns (opts :disp-ns) :blur) field-id]))
                  :on-change (fn [e]
                               (let [value (on-change-transform (.. e -target -value))]
                                 (reset! value-atom value)
                                 (dispatch [(with-ns (opts :disp-ns) :update-value) field-id value])))}]]
        (when (= :invalid status)
          [:div.error-message (first errors)])
        (when (opts :help-text)
          [:div.explanation (opts :help-text)])]])))

(defn button-view [opts]
  (let [fields-valid?
        @(subscribe [:braid.core.client.gateway.forms.user-auth.subs/fields-valid?
                     (opts :validations)])]
    [:button.submit
     {:type "submit"
      :class (when-not fields-valid? "disabled")}
     (opts :text)]))

(defn form-view [opts & body]
  [:div.section
   {:class (str (opts :class) " "
                (when (opts :disabled?) "disabled"))}
   [:form
    {:no-validate true
     :on-submit (opts :on-submit)}

    (into [:fieldset {:disabled (opts :disabled?)}]
          (concat [(when (opts :title) [:h1 (opts :title)])] body))]])
