(ns braid.client.quests.helpers
  (:require [braid.client.quests.list :refer [quests]]))

; getters

(defn get-active-quest-records [state]
  (->> state
       :quest-records
       vals
       (filter (fn [quest-record]
                 (= (quest-record :state) :active)))))

(defn get-next-quest [state]
  (let [quest-ids-with-records (->> state
                                    :quest-records
                                    vals
                                    (map (fn [quest-record]
                                           (quest-record :quest-id)))
                                    set)
        next-quest (->> quests
                        (remove (fn [quest]
                                  (contains? quest-ids-with-records (quest :id))))
                        first)]
    next-quest))

; setters

(defn store-quest-record [state quest-record]
  (assoc-in state [:quest-records (quest-record :id)] quest-record))

(defn complete-quest [state quest-record-id]
  (assoc-in state [:quest-records quest-record-id :state] :complete))

(defn skip-quest [state quest-record-id]
  (assoc-in state [:quest-records quest-record-id :state] :skipped))

(defn increment-quest [state quest-record-id]
  (update-in state [:quest-records quest-record-id :progress] inc))

