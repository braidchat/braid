(ns braid.base.server.http-client-routes
  (:require
    [braid.base.conf :refer [config]]
    [compojure.core :refer [GET defroutes]]
    [compojure.route :refer [resources]]
    [ring.util.response :refer [resource-response]]))

(defroutes resource-routes

  ; add cache-control headers to js files)
  ; (since it uses a cache-busted url anyway)
  (GET (str "/js/:build{desktop|mobile|gateway|base}.js") [build]
    (if (boolean (config :prod-js))
      (if-let [response (resource-response (str "public/js/prod/" build ".js"))]
        (assoc-in response [:headers "Cache-Control"] "max-age=365000000, immutable")
        {:status 404
         :body "File Not Found"})
      (if-let [response (resource-response (str "public/js/dev/" build ".js"))]
        response
        {:status 200
         :headers {"Content-Type" "application/javascript"}
         :body (str "alert('The " build " js files are missing. Please compile them with cljsbuild or figwheel.');")})))

  (resources "/"))
