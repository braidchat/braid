(ns braid.emoji.client.views
  (:require
   [braid.core.client.s3 :as s3]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [clojure.string :as string]))

(defn new-custom-emoji-view
  []
  (let [shortcode (r/atom "")
        uploading? (r/atom false)
        image-url (r/atom nil)]
    (fn []
      [:div.new-emoji
       [:label "Shortcode (omit ':')"
        [:input {:placeholder "shortcode"
                 :value @shortcode
                 :on-change (fn [e]
                              (reset! shortcode
                                      (.. e -target -value)))}]]
       [:label {:style {:border "1px solid gray"
                        :background-color "white"
                        :padding "0.25em"
                        :border-radius "5px"}}
        (if @uploading? "Uploading..." "Upload Image")
        [:input {:type "file"
                 :multiple false
                 :style {:display "none"}
                 :on-change (fn [e]
                              (reset! uploading? true)
                              (s3/upload
                                (aget (.. e -target -files) 0)
                                (fn [url]
                                  (reset! uploading? false)
                                  (reset! image-url url))))}]]
       [:button
        {:disabled (or (string/blank? @shortcode)
                       (string/blank? @image-url))
         :on-click (fn [_]
                     (dispatch [:emoji/add-emoji
                                {:group-id @(subscribe [:open-group-id])
                                 :shortcode @shortcode
                                 :image @image-url}]))}
        "Add"]
       [:br]
       (when-let [url @image-url]
         [:img {:src url :width "300"}])])))

(defn extra-emoji-settings-view
  [group]
  [:div.settings.custom-emoji
   [:h2 "Custom Emoji"]
   [new-custom-emoji-view]
   (if-let [emojis (seq @(subscribe [:emoji/group-emojis (group :id)]))]
     [:table
      [:thead
       [:tr [:th "shortcode"] [:th ""]]]
      [:tbody
       (for [emojo emojis]
         ^{:key (emojo :id)}
         [:tr
          [:td (str ":" (emojo :shortcode) ":")]
          [:td [:img {:src (emojo :image)}]]])]]
     [:p "No custom emoji yet"])])
