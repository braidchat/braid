(ns braid.core.client.ui.views.thread-header
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [run! reaction]]
    [braid.core.hooks :as hooks]
    [braid.core.client.helpers :as helpers]
    [braid.core.client.ui.views.mentions :refer [user-mention-view tag-mention-view]]
    [braid.lib.color :as color]
    [braid.popovers.helpers :as popovers]))

(defn tag-option-view
  [tag thread-id selected?]
  [:div.tag-option
   {:on-click (fn []
                (popovers/close!)
                (dispatch [:add-tag-to-thread {:thread-id thread-id
                                               :tag-id (tag :id)}]))
    :class (when selected? "selected")
    :ref (fn [ref] (when (and selected? ref) (.scrollIntoView ref false)))}
   [:div.rect {:style {:background (color/->color (tag :id))}}]
   [:span {:style {:color (color/->color (tag :id))}}
    "#" (tag :name)]])

(defn user-option-view
  [user thread-id selected?]
  [:div.user-option
   {:on-click (fn []
                (popovers/close!)
                (dispatch [:add-user-to-thread {:thread-id thread-id
                                                :user-id (user :id)}]))
    :class (when selected? "selected")
    :ref (fn [ref] (when (and selected? ref) (.scrollIntoView ref false)))}
   [:div.rect {:style {:background (color/->color (user :id))}}]
   [:span {:style {:color (color/->color (user :id))}}
    "@" (user :nickname)]])

(defn add-tag-user-popover-view
  [thread]
  (let [search-query (r/atom "")
        selected-column (r/atom 0)
        selected-row (r/atom 0)]
    (fn [thread]
      (let [thread-tag-ids (set (thread :tag-ids))
            thread-user-ids (set (thread :mentioned-ids))
            tags (reaction
                   (->> @(subscribe [:open-group-tags])
                       (remove (fn [tag]
                                 (contains? thread-tag-ids (tag :id))))
                       (filter (fn [tag]
                                 (or (string/blank? @search-query)
                                     (string/includes? (tag :name) @search-query))))
                       (sort-by :name)
                       vec))
            users (reaction
                    (->> @(subscribe [:users])
                        (remove (fn [user] (contains? thread-user-ids (user :id))))
                        (filter (fn [user]
                                  (or (string/blank? @search-query)
                                      (string/includes? (user :nickname) @search-query))))
                        (sort-by :nickname)
                        vec))
            selected (reaction (get-in [@tags @users] [@selected-column @selected-row]))]
        (run!
          (let [counts (mapv count [@tags @users])
                cur-count (get counts @selected-column)
                other-count (get counts (mod (inc @selected-column) 2))]
            (if (and (zero? cur-count)
                     (not (zero? other-count)))
              (do
                (swap! selected-column (comp #(mod % 2) inc))
                (swap! selected-row #(max 0 (min % (dec other-count)))))
              (swap! selected-row #(max 0 (min % (dec cur-count)))))))
        [:div.add-mention-popup
         [:input.search
          {:placeholder "Search for tag/user to add to thread"
           :auto-focus true
           :value @search-query
           :on-change (fn [e] (reset! search-query
                                     (.. e -target -value)))
           :on-key-down
           (fn [e] (when-let [k (#{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight"
                                 "Escape" "Enter"}
                                 (.-key e))]
                    (.preventDefault e)
                    (.stopPropagation e)
                    (case k
                      ("ArrowRight" "ArrowLeft")
                      (swap! selected-column (comp #(mod % 2) inc))

                      "ArrowUp"
                      (swap! selected-row (comp (partial max 0) dec))

                      "ArrowDown"
                      ;; not guarding this because the above `run!`
                      ;; will make sure that it stays in bounds
                      (swap! selected-row inc)

                      "Enter"
                      (when @selected
                        (popovers/close!)
                        (case @selected-column
                          0 (dispatch [:add-tag-to-thread
                                       {:thread-id (thread :id)
                                        :tag-id (@selected :id)}])
                          1 (dispatch [:add-user-to-thread
                                       {:thread-id (thread :id)
                                        :user-id (@selected :id)}])))

                      "Escape" (popovers/close!))))}]
         [:div.search-results

          [:div.tag-list
           (if (seq @tags)
             (doall
               (for [tag @tags]
                 ^{:key (tag :id)}
                 [tag-option-view tag (thread :id) (= tag @selected)]))
             [:div.name "No unused tags matching"])]

          [:div.user-list
           (if (seq @users)
             (doall
               (for [user @users]
                 ^{:key (user :id)}
                 [user-option-view user (thread :id) (= user @selected)]))
             [:div.name "No unmentioned users matching"])]]

         [:button.cancel {:on-click (fn [_] (popovers/close!))} "Cancel"]]))))

(defn add-tag-button-view [thread]
  [:div.add
   [:span.pill {:on-click (popovers/on-click
                            (fn []
                              [add-tag-user-popover-view thread]))}
    "+"]])

(defn thread-tags-view [thread]
  [:div.tags
   (doall
     (for [user-id (thread :mentioned-ids)]
       ^{:key user-id}
       [user-mention-view user-id]))
   (doall
     (for [tag-id (thread :tag-ids)]
       ^{:key tag-id}
       [tag-mention-view tag-id]))
   (when-not (:readonly thread)
     [add-tag-button-view thread])])

(def header-item-dataspec
  {:priority number?
   :view fn?})

(defonce thread-header-items
  (hooks/register! (atom []) [header-item-dataspec]))

(def thread-control-dataspec
  {:priority number?
   :view fn?})

(defonce thread-controls
  (hooks/register!
    (atom [{:view
            (fn [thread]
              [:div.control.mute
               {:title "Mute"
                :on-click (fn [e]
                            ; Need to preventDefault & propagation when using
                            ; divs as controls, otherwise divs higher up also
                            ; get click events
                            (helpers/stop-event! e)
                            (dispatch [:unsub-thread
                                       {:thread-id (thread :id)}]))}
               \uf1f6])
            :priority 0}])
    [thread-control-dataspec]))

(defn thread-header-view [thread]
  (into
    [:div.head]
    (conj
      (->> @thread-header-items
           (sort-by :priority)
           reverse
           (mapv (fn [el]
                   [(el :view) thread])))

      [:div.controls
       [:div.main
        (if @(subscribe [:thread-open? (thread :id)])
          [:div.control.close
           {:title "Close"
            :tabIndex 0
            :role "button"
            :on-click (fn [e]
                        ; Need to preventDefault & propagation when using
                        ; divs as controls, otherwise divs higher up also
                        ; get click events
                        (helpers/stop-event! e)
                        (dispatch [:hide-thread! {:thread-id (thread :id)}]))}
           \uf00d]
          [:div.control.unread
           {:title "Mark Unread"
            :tabIndex 0
            :role "button"
            :on-click (fn [e]
                        ; Need to preventDefault & propagation when using
                        ; divs as controls, otherwise divs higher up also
                        ; get click events
                        (helpers/stop-event! e)
                        (dispatch [:reopen-thread! (thread :id)]))}
           \uf0e2])]

       (into [:div.extras]
             (->> @thread-controls
                  (sort-by :priority)
                  reverse
                  (mapv (fn [el]
                          [(el :view) thread]))))]

      [thread-tags-view thread])))
