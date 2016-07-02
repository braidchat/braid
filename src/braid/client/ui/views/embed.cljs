(ns braid.client.ui.views.embed
  (:require [reagent.core :as r]
            [chat.client.xhr :refer [edn-xhr]]
            [braid.client.helpers :refer [->color url->color]]))

(defn- arr->rgb [arr]
  ; until embedly provides color alpha, default to transparent background
  "rgba(255,255,255,0)"
  #_(if arr
    (str "rgb(" (arr 0) "," (arr 1) "," (arr 2) ")")
    "rgb(150, 150, 150)"))

(defn- website-embed-view [content]
  [:div.content.loaded.website
   {:style {:background-color (url->color (content :original_url))}}
   (if-let [img (get-in content [:images 0])]
     [:img.image {:src (img :url)
                  :style {:background-color
                          (arr->rgb (get-in img [:colors 0 :color]))}}]
     [:img.image {:src (:favicon_url content)}])
   [:div.about
    [:div.provider
     [:div.favicon {:style {:background-image
                            (str "url(" (:favicon_url content) ")")}}]
     [:div.name (:provider_name content)]]
    [:div.title (:title content)]
    [:div.url (:url content)]]])

(defn- video-overlay-view [content]
  [:div.content
   [:div.frame
    {:dangerouslySetInnerHTML {:__html (:html (:media content))}}]])

(defn- content-overlay-view [content])

(defn- video-embed-view [content]
  [:div.video
   (when-let [img (get-in content [:images 0])]
     [:img {:src (img :url)
            :style {:background-color
                    (arr->rgb (get-in img [:colors 0 :color]))}}])])

(defn- image-embed-view [content]
  [:div.image
   (when-let [img (get-in content [:images 0])]
     [:img {:src (img :url)
            :style {:background-color
                    (arr->rgb (get-in img [:colors 0 :color]))}}])])

(defn embed-view [url]
  (let [content (r/atom {})
        set-content! (fn [response]
                      (reset! content response))
        fetch-content! (fn [url]
                         (when (some? url)
                           (edn-xhr {:method :get
                                     :uri "/extract"
                                     :params {:url (js/encodeURIComponent url)}
                                     :on-complete set-content!})))]
    (r/create-class
      {:component-did-mount
       (fn []
         (fetch-content! url))

       :reagent-render
       (fn []
         [:div.embed
          (if (:type @content)
            [:div.content.loaded {:on-click (fn []
                                              (.open js/window (:original_url @content)))}
             (cond
               (= "video" (get-in @content [:media :type]))
               [video-embed-view @content]
               (= "photo" (get-in @content [:media :type]))
               [image-embed-view @content]
               (@content :url)
               [website-embed-view @content]
               :else
               nil)]
            [:div.content.loading])])})))
