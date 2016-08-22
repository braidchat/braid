(ns braid.client.ui.views.sidebar
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [re-frame.core :refer [dispatch subscribe]]
            [braid.client.helpers :refer [id->color location element-offset
                                          get-style]]
            [braid.client.routes :as routes]
            [braid.client.ui.styles.vars :as style-vars]
            [goog.events :as events]
            [goog.style :as gstyle])
  (:import [goog.events EventType]))

(def v- (partial mapv -))

(def v+ (partial mapv +))

(defn px->n [px]
  (-> (string/replace px #"px$" "")
      (js/parseInt 10)))

(defn css->str
  [css-unit]
  (str (:magnitude css-unit) (name (:unit css-unit))))

(defn index-of
  [col e]
  (some (fn [[idx e']] (when (= e' e) idx)) (map-indexed vector col)))

(defn clamp
  [bot x top]
  {:pre [(< bot top)]}
  (max bot (min x top)))

(defn move-idx
  [v old-i new-i]
  {:post [(= (count %) (count v))]}
  (let [[old-i new-i] (map #(clamp 0 % (dec (count v))) [old-i new-i])]
    (if (= old-i new-i)
      v
      (let [em (nth v old-i)]
        (->> v
             (map-indexed vector)
             (reduce (fn [v' [idx e]]
                       (cond
                         (= idx old-i) v'
                         (= idx new-i) (if (< old-i new-i)
                                         (conj v' e em)
                                         (conj v' em e))
                         :else (conj v' e)))
                     []))))))

(defn badge-view [group-id]
  (let [cnt (subscribe [:group-unread-count group-id])]
    (fn []
      (if (and @cnt (> @cnt 0))
        [:div.badge @cnt]
        [:div]))))

(defn groups-view []
  (let [active-group (subscribe [:active-group])
        ordered-groups (subscribe [:ordered-groups])
        move-group-by (fn [group by]
                        (let [current-groups (mapv :id @ordered-groups)
                              old-idx (index-of current-groups (group :id))
                              new-idx (+ old-idx by)
                              new-groups (move-idx current-groups old-idx new-idx)]
                          (dispatch [:set-preference [:groups-order new-groups]])))
        drag-state (r/atom {:grp nil
                            :elt-height 0
                            :click nil
                            :listeners nil
                            :start nil
                            :location nil
                            :offset nil})
        drag-move (fn [grp evt]
                    (when (= grp (@drag-state :grp))
                      (swap! drag-state assoc
                             :location (v+ (location evt)
                                           (:offset @drag-state)))))
        drag-end (fn [grp evt]
                   (when-let [[up move] (@drag-state :listeners)]
                     (doto js/window
                       (events/unlisten EventType.MOUSEUP up)
                       (events/unlisten EventType.MOUSEMOVE move)))
                   (let [y-delta (-> (v- (@drag-state :location)
                                         (@drag-state :start))
                                     second)
                         idx-delta (-> y-delta
                                       (/ (@drag-state :elt-height))
                                       js/Math.round)]
                     (if (< (js/Math.abs y-delta) (/ (@drag-state :elt-height) 2))
                       (when-let [click (@drag-state :click)]
                         (click))
                       (move-group-by grp idx-delta)))
                   (swap! drag-state assoc
                          :grp nil
                          :listeners nil))
        drag-start (fn [grp evt]
                     (let [loc (location evt)
                           elt (loop [elt (.-target evt)]
                                 (if (= "A" (.-tagName elt))
                                   elt
                                   (recur (.-parentElement elt))))
                           offset (element-offset elt)
                           elt-height (+ (.-clientHeight elt)
                                         (-> elt
                                             (get-style "margin-bottom")
                                             px->n
                                             (* 2)))
                           up (partial drag-end grp)
                           move (partial drag-move grp)]
                       (swap! drag-state assoc
                              :grp grp
                              :start loc
                              :click (fn [] (.click elt))
                              :elt-height elt-height
                              :location offset
                              :offset (v- offset loc)
                              :listeners [up move])
                       (doto js/window
                         (events/listen EventType.MOUSEUP up)
                         (events/listen EventType.MOUSEMOVE move))))]
    (fn []
      [:div.groups
       (doall
         (for [group @ordered-groups]
           ^{:key (group :id)}
           [:a.group.option
            {:class (when (= (:id @active-group) (:id group)) "active")
             :style (merge
                      {:background-color (id->color (group :id))}
                      (when-let [avatar (group :avatar)]
                        {:background-image (str "url(" avatar ")")
                         :background-position "center"
                         :background-size "100%"})
                      (when (= group (@drag-state :grp))
                        {:opacity 0.2}))
             :title (group :name)
             :href (routes/page-path {:group-id (group :id)
                                      :page-id "inbox"})
             :on-mouse-down (partial drag-start group)
             :on-mouse-up (partial drag-end group)
             :on-mouse-move (partial drag-move group)}
            (when-not (group :avatar)
              (string/join "" (take 2 (group :name))))
            [badge-view (group :id)]]))
       (when-let [drag-grp (@drag-state :grp)]
         [:a.group.option
          {:style (merge {:background-color (id->color (drag-grp :id))
                          :position "absolute"
                          :left (css->str style-vars/pad)
                          :top (second (@drag-state :location))
                          :z-index 1000
                          :width (css->str style-vars/top-bar-height)
                          :height (css->str style-vars/top-bar-height)}
                         (when-let [avatar (drag-grp :avatar)]
                           {:background-image (str "url(" avatar ")")
                            :background-position "center"
                            :background-size "100%"}))}
          (when-not (drag-grp :avatar)
            (string/join "" (take 2 (drag-grp :name))))])])))

(defn group-explore-button-view []
  (let [page (subscribe [:page])
        invitations (subscribe [:invitations])]
    (fn []
      [:a.option.other.plus
       {:class (when (= (@page :type) :group-explore) "active")
        :href (routes/other-path {:page-id "group-explore"})}
       (when (< 0 (count @invitations))
         [:div.badge (count @invitations)])])))


(defn global-settings-button-view []
  (let [page (subscribe [:page])]
    (fn []
      [:a.option.other.global-settings
       {:class (when (= (@page :type) :global-settings) "active")
        :href (routes/other-path {:page-id "global-settings"})}])))

(defn sidebar-view []
  [:div.sidebar
   [groups-view]
   [group-explore-button-view]
   [global-settings-button-view]])
