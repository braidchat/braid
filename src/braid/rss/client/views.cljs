(ns braid.rss.client.views
  (:require
   [braid.core.client.ui.views.pills :as pills]
   [clojure.string :as string]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch subscribe]]))

(defn new-rss-feed-view
  [group]
  (let [first-tag-id (:id (first @(subscribe [:tags-in-group (group :id)])))
        new-feed (r/atom {:feed-url ""
                          :tag-ids #{first-tag-id}})
        error (r/atom nil)]
    (fn [group]
      [:form.new-rss-feed
       {:on-submit
        (fn [e]
          (.preventDefault e)
          (cond
            (string/blank? (:feed-url @new-feed))
            (reset! error "You need to supply a valid feed URL")

            :else
            (do
              (dispatch [:rss/add-feed (assoc @new-feed :group-id (group :id))])
              (swap! new-feed assoc :feed-url ""))))}
       [:div.warning "New posts will be posted as coming from you"]
       (when @error [:div.error @error])
       [:label [:span "Feed URL"]
        [:br]
        [:input {:type "url"
                 :placeholder "https://foo.bar/rss.xml"
                 :value (:feed-url @new-feed)
                 :on-change (fn [e] (swap! new-feed assoc :feed-url (.. e -target -value)))}]]
       [:label [:span "Tags to apply to posts"]
        [:br]
        ;; Annoying React thing: multiple select doesn't seem to be
        ;; very well supported, so we can't just set the value of the
        ;; :select, but instead rely on reading from the on-change and
        ;; determining what has been selected
        ;; This is also why we set the tag-ids to default to the first
        ;; tag in the group, because it shows the first group as
        ;; highlighed, we don't seem to have a good way to unhighlight
        ;; it, and it's confusing otherwise
        [:select {:multiple true
                  :on-change (fn [e]
                               (->> (.. e -target -selectedOptions)
                                   js/Array.from
                                   seq
                                   (into #{} (map (fn [elt] (uuid (.-value elt)))))
                                   (swap! new-feed assoc :tag-ids)))}
         (doall (for [tag @(subscribe [:tags-in-group (group :id)])
                      :let [tag-id (tag :id)]]
                  ^{:key tag-id}
                  [:option {:value tag-id} (tag :name)]))]]
       (when (empty? (:tag-ids @new-feed))
         [:div.warning
          [:p "You currently have no tags selected to be applied to incoming feed items"]
          [:p "This is maybe what you want, if you'd like to use Braid as a "
           " private feed-reader, or you want to manually vet items before tagging them."]
          [:p "Otherwise, you can select one or more of the above tags"
           " to allow others to automatically see them."]])
       [:input {:type "submit" :value "Add"
                :disabled (string/blank? (:feed-url @new-feed))}]])))

(defn existing-feeds-view
  [group]
  (if-let [feeds @(subscribe [:rss/feeds (group :id)])]
    [:table
     [:thead [:tr [:th "URL"] [:th "User"] [:th "Tags"] [:th]]]
     [:tbody
      (doall (for [feed feeds]
               ^{:key (feed :id)}
               [:tr
                [:td (feed :feed-url)]
                [:td [pills/user-pill-view (feed :user-id)]]
                [:td (doall (for [tag-id (feed :tag-ids)]
                              ^{:key tag-id}
                              [pills/tag-pill-view tag-id]))]
                [:td [:button
                      {:style {:font-family "fontawesome"
                               :size "smaller"
                               :color "red"
                               :cursor "pointer"}
                       :on-click (fn [_] (dispatch [:rss/retract-feed feed]))}
                      \uf1f8]]]))]]
    [:p "No feeds added to this group yet"]))

(defn rss-feed-settings-view
  [group]
  (dispatch [:rss/load-group-feeds (group :id)])
  [:div.settings.rss-feeds
   [:h2 "RSS Feeds"]
   [new-rss-feed-view group]
   [existing-feeds-view group]])
