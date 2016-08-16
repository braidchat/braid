(ns braid.client.quests.helpers)

; getters

(defn get-active-quests [state]
  (->> state
       :quests
       vals
       (filter (fn [quest] (= :active (quest :state))))))

; setters

(defn complete-quest [state quest-id]
  (assoc-in state [:quests quest-id :state] :complete))

(defn skip-quest [state quest-id]
  (assoc-in state [:quests quest-id :state] :skipped))

(defn activate-next-quest [state]
  (let [quest-id-to-activate (->> state
                                  :quests
                                  vals
                                  (filter (fn [quest]
                                            (= (quest :state) :inactive)))
                                  first
                                  :id)]
    (assoc-in state [:quests quest-id-to-activate :state] :active)))
