(ns braid.mobile.state
  (:require [re-frame.core :as rf]
            [reagent.ratom :include-macros true :refer-macros [reaction]]))

; login

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
      :active-group-id 1
      :threads {4 {:id 4
                   :group-id 1
                   :messages [{:id 400 :content "foo"}
                              {:id 401 :content "bar"}
                              {:id 402 :content "baz"}]}
                5 {:id 5
                   :group-id 1
                   :messages [{:id 501 :content "aaa"}
                              {:id 502 :content "bbb"}
                              {:id 503 :content "ccc"}]}
                6 {:id 6
                   :group-id 2
                   :messages [{:id 504 :content "xoo"}
                              {:id 505 :content "xar"}
                              {:id 506 :content "xaz"}]}})))

(rf/register-sub :logged-in?
  (fn [state _]
    (reaction (boolean (:user @state)))))

(rf/register-sub :groups
  (fn [state _]
    (reaction (vals (:groups @state)))))

; current group

(rf/register-sub :active-group
  (fn [state _]
    (let [group-id (reaction (:active-group-id @state))]
      (reaction (get-in @state [:groups @group-id])))))

(rf/register-sub :active-group-inbox-threads
  (fn [state _]
    (let [group-id (reaction (:active-group-id @state))
          threads (reaction (vals (:threads @state)))]
      (reaction (filter (fn [t] (= @group-id (t :group-id)))
                        @threads)))))

(rf/register-handler :set-active-group-id!
  (fn [state [_ group-id]]
    (assoc state :active-group-id group-id)))
