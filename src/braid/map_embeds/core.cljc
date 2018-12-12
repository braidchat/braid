(ns braid.map-embeds.core
  "Detects google maps links and includes embeds a static image"
  (:require
   [braid.embeds.api :as embeds]
   [braid.core.api :as core]
   #?@(:clj
       [[org.httpkit.client :as http]
        [braid.core.server.conf :refer [config]]])))

(def google-regex #"^https://www.google.com/maps/.*/@\-?(\d+\.\d+),\-?(\d+.\.\d+).*")

#?(:cljs
   (defn- map-embed-view
     [url]
     (let [[_ lat lng] (re-matches google-regex url)]
       [:a
        {:href (str url)
         :target "_blank"
         :rel "noopener noreferrer"}
        [:img
         {:src (str "//" (aget js/window "api_domain") "/maps-embeds/static-map?lat=" lat "&lng=" lng)}]])))


(defn init! []
  #?(:clj
     (do
       (core/register-config-var! :google-maps-api-key)

       (core/register-private-http-route!
        [:get "/maps-embeds/static-map"
         (fn [request]
           (let [{:keys [lat lng]} (request :params)]
             (if-let [key (config :google-maps-api-key)]
               {:status 200
                :body (:body
                       @(http/request
                         {:method :get
                          :url (str "https://maps.googleapis.com/maps/api/staticmap?center=" lat "," lng "&key=" key "&size=300x200&zoom=13")}))}
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


