(ns braid.ui.views.embed
  (:require [reagent.core :as r]
            [chat.client.xhr :refer [edn-xhr]]))

(defn- arr->rgb [arr]
  (str "rgb(" (arr 0) "," (arr 1) "," (arr 2) ")"))

(defn- website-embed-view [content]
  (let [img (get-in content [:images 0])]
    [:div.content.loaded.website
     [:img.image {:src (img :url)
                  :style {:background-color
                          (arr->rgb (get-in img [:colors 0 :color]))}}]

     [:div.about
      [:div.provider
       [:div.favicon {:style {:background-image
                              (str "url(" (:favicon_url content) ")")}}]
       [:div.name (:provider_name content)]]
      [:div.title (:title content)]
      [:div.url (:url content)]]]))

(defn- video-overlay-view [content]
  [:div.content
   [:div.frame
    {:dangerouslySetInnerHTML {:__html (:html (:media content))}}]])

(defn- content-overlay-view [content])

(defn- video-embed-view [content]
  [:div.video
   (let [img (get-in content [:images 0])]
     [:img {:src (img :url)
            :style {:background-color
                    (arr->rgb (get-in img [:colors 0 :color]))}}])])

(defn- image-embed-view [content]
  [:div.image
   (let [img (get-in content [:images 0])]
     [:img {:src (img :url)
            :style {:background-color
                    (arr->rgb (get-in img [:colors 0 :color]))}}])])

(defn embed-view []
  (let [content (r/atom {})
        set-content! (fn [response]
                      (reset! content response))
        fetch-content! (fn [url]
                         (when (seq url)
                           (edn-xhr {:method :get
                                     :uri "/extract"
                                     :params {:url url}
                                     :on-complete set-content!})))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (let [url (:url (r/props this))]
           (fetch-content! url)))

       :component-did-update
       (fn [this]
         (let [url (:url (r/props this))]
           (fetch-content! url)))

       :reagent-render
       (fn []
         [:div.embed
          (if (:type @content)
            [:div.content.loaded {:on-click (fn []
                                              (.open js/window (:url @content)))}
             (cond
               (= "video" (get-in @content [:media :type]))
               [video-embed-view @content]
               (= "photo" (get-in @content [:media :type]))
               [image-embed-view @content]
               :else
               [website-embed-view @content])]
            [:div.content.loading])])})))
