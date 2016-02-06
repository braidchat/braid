(require '[figwheel-sidecar.repl :as r]
         '[figwheel-sidecar.repl-api :as ra])

(ra/start-figwheel!

  {:figwheel-options {:css-dirs ["resources/public/css"]
                      :reload-clj-files {:clj false :cljc false}
                      :server-port 3559}
   :build-ids ["dev"]
   :all-builds
   [{:id "dev"
     :figwheel true
     :source-paths ["src/chat/client" "src/chat/shared"]
     :compiler {:main 'chat.client.core
                :asset-path "/js/out"
                :output-to "resources/public/js/out/chat.js"
                :output-dir "resources/public/js/out"
                :verbose true}}]})

(ra/cljs-repl)

