(ns braid.mobile.state
  (:require [re-frame.core :as rf]
            [reagent.ratom :include-macros true :refer-macros [reaction]]))

(rf/register-handler :open-sidebar!
  (fn [state _]
    (assoc-in state [:views :sidebar :open?] true)))

(rf/register-handler :close-sidebar!
  (fn [state _]
    (assoc-in state [:views :sidebar :open?] false)))

(rf/register-handler :log-in!
  (fn [state _]
    (assoc state
      :user {}
      :groups {1 {:id 1
                  :name "Foo"}
               2 {:id 2
                  :name "Bar"}
               3 {:id 3
                  :name "Baz"}}
      :current-group-id 1
      :threads {4 {:id 4
                   :group-id 1
                   :messages [{:id 400 :content "foo"}
                              {:id 401 :content "bar"}
                              {:id 402 :content "baz"}]}
                5 {:id 5
                   :group-id 1
                   :messages [{:id 501 :content "foo"}
                              {:id 502 :content "bar"}
                              {:id 503 :content "baz"}]}
                6 {:id 6
                   :group-id 2
                   :messages [{:id 504 :content "foo"}
                              {:id 505 :content "bar"}
                              {:id 506 :content "baz"}]}})))

(rf/register-sub :logged-in?
  (fn [state _]
    (reaction (boolean (:user @state)))))

(rf/register-sub :groups
  (fn [state _]
    (reaction (vals (:groups @state)))))

(rf/register-sub :threads
  (fn [state _]
    (reaction (vals (:threads @state)))))

(rf/register-sub :sidebar-open?
  (fn [state _]
    (reaction (get-in @state [:views :sidebar :open?]))))
