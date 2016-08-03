(ns braid.client.ui.views.embed
  (:require [reagent.core :as r]
            [cljs.core.async :refer [put!]]
            [braid.client.xhr :refer [edn-xhr]]
            [braid.client.helpers :refer [->color url->color]]))

(defn- arr->rgb [arr]
  ; until embedly provides color alpha, default to transparent background
  "rgba(255,255,255,0)"
  #_(if arr
    (str "rgb(" (arr 0) "," (arr 1) "," (arr 2) ")")
    "rgb(150, 150, 150)"))

(defn website-embed-view
  "View for embedly generic website info. Prefer to use embed-view with a url
  instead of this directly"
  [content]
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

(defn video-embed-view
  "View for embedly video info. Prefer to use embed-view with a url instead of
  this directly"
  [content]
  [:div.video
   (when-let [img (get-in content [:images 0])]
     [:img {:src (img :url)
            :style {:background-color
                    (arr->rgb (get-in img [:colors 0 :color]))}}])])

(defn image-embed-view
  "View for embedly image info. Prefer to use embed-view with a url instead of
  this directly"
  [content]
  [:div.image
   (when-let [img (get-in content [:images 0])]
     [:img {:src (img :url)
            :style {:background-color
                    (arr->rgb (get-in img [:colors 0 :color]))}}])])

(defn embed-view [url embed-update-chan]
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

       :component-did-update
       (fn [_]
         (when embed-update-chan
           (put! embed-update-chan (js/Date.))))

       :reagent-render
       (fn [_ _]
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
