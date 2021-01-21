(ns braid.map-embeds.core
  "Detects google maps links and includes embeds a static image"
  (:require
   [braid.base.api :as base]
   [braid.embeds.api :as embeds]
   #?@(:clj
       [[org.httpkit.client :as http]
        [braid.base.conf :refer [config]]]
       :cljs
       [[goog.object :as o]])))

(def google-regex #"^https://www.google.com/maps/.*/@(\-?\d+\.\d+),(\-?\d+.\.\d+).*")

#?(:cljs
   (defn- map-embed-view
     [url]
     (let [[_ lat lng] (re-matches google-regex url)]
       [:a
        {:href (str url)
         :target "_blank"
         :rel "noopener noreferrer"}
        [:img
         {:src (str "//" (o/get js/window "api_domain") "/maps-embeds/static-map?lat=" lat "&lng=" lng)}]])))


(defn init! []
  #?(:clj
     (do
       (base/register-config-var! :google-maps-api-key)

       (base/register-private-http-route!
        [:get "/maps-embeds/static-map"
         (fn [request]
           (let [{:keys [lat lng]} (request :params)]
             (if-let [key (config :google-maps-api-key)]
               (let [resp @(http/request
                             {:method :get
                              :url (str "https://maps.googleapis.com/maps/api/staticmap?center=" lat "," lng "&key=" key "&size=300x200&zoom=16")})]
                 {:status 200
                  :headers {"Content-Type" (get-in resp [:headers :content-type])
                            "Content-Length" (get-in resp [:headers :content-length])}
                  :body (:body resp)})
               {:status 500
                :body nil})))]))

     :cljs
     (do
       (embeds/register-embed!
        {:handler
         (fn [{:keys [urls]}]
           (when-let [url (->> urls
                               (some (fn [url]
                                       (first (re-matches google-regex url)))))]
             [map-embed-view url]))

         :styles
         [:>.image
          [:>img
           {:width "100%"}]]

         :priority 1}))))
