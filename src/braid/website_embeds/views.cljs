(ns braid.website-embeds.views
  (:require
    [braid.lib.color :as color]
    [braid.lib.xhr :refer [edn-xhr]]
    [reagent.core :as r]))

(defn- website-embed-view
  [content]
  [:div.website
   {:style {:background-color (color/url->color (content :original_url))}}
   (if-let [img (get-in content [:images 0])]
     [:img.image {:src (img :url)}]
     [:img.image {:src (:favicon_url content)}])
   [:div.about
    [:div.provider
     [:div.favicon {:style {:background-image
                            (str "url(" (:favicon_url content) ")")}}]
     [:div.name (:provider_name content)]]
    [:div.title (:title content)]
    [:div.url (:url content)]]])

(defn- video-embed-view
  [content]
  [:div.video
   (when-let [img (get-in content [:images 0])]
     [:img {:src (img :url)}])])

(defn- image-embed-view
  [content]
  [:div.image
   (when-let [img (get-in content [:images 0])]
     [:img {:src (img :url)}])])

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
       (fn [_ _]
         (if-let [media-type (:type @content)]
           [:div.website-embed.loaded
            {:on-click (fn []
                         (.open js/window (:original_url @content)))}
            (cond
              (= "video" media-type)
              [video-embed-view @content]

              (= "photo" media-type)
              [image-embed-view @content]

              (@content :url) [website-embed-view @content])]
           [:div.website-embed.loading]))})))

(defn handler
  [{:keys [urls]}]
  (when-let [url (first urls)]
    [embed-view url]))
