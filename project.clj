(defproject chat "0.0.1"
  :source-paths ["src"]

  :dependencies [;server
                 [org.clojure/clojure "1.7.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [commons-codec "1.10"]
                 [http-kit "2.1.19"]
                 [ring/ring-defaults "0.1.5"]
                 [fogus/ring-edn "0.3.0"]
                 [compojure "1.4.0"]
                 [environ "1.0.0"]
                 [com.taoensso/sente "1.5.0"]
                 [com.taoensso/timbre "4.0.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [crypto-password "0.1.3"]
                 [joda-time "2.8.2"]
                 [instaparse "1.4.1"]
                 [com.taoensso/carmine "2.11.1"]
                 ;client
                 [org.clojure/clojurescript "1.7.145"]
                 [org.omcljs/om "0.8.8"]
                 [org.clojars.leanpixel/cljs-utils "0.2.1"]
                 [prismatic/schema "0.4.3"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/sente "1.5.0"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [org.clojars.leanpixel/om-fields "1.8.6"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]]

  :main chat.server.handler
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.0.6"]
            [jamesnvc/lein-lesscss "1.4.0"] ]

  :repl-options {:timeout 120000}
  :clean-targets ^{:protect false}
  ["resources/public/js/out"
   ;"resources/public/css/out" ; need to fix lein-lesscss to create the director if it doesn't exist
   ]
  :lesscss-paths ["resources/less"]
  :lesscss-output-path "resources/public/css/out"
  :cljsbuild {:builds
              [{:id "release"
                :source-paths ["src/chat/client" "src/chat/shared"]
                :compiler {:main chat.client.core
                           :asset-path "/js/out"
                           :output-to "resources/public/js/out/chat.js"
                           :output-dir "resources/public/js/out"
                           :optimizations :advanced
                           :pretty-print false }}]}

  :min-lein-version "2.5.0"

  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.9.5201" :exclusions [joda-time]]
                                  [figwheel-sidecar "0.4.0"]]}
             :prod {:dependencies [[com.datomic/datomic-pro "0.9.5201" :exclusions [joda-time]]
                                   [org.postgresql/postgresql "9.3-1103-jdbc4"]
                                   [clj-aws-s3 "0.3.10" :exclusions [joda-time]]
                                   [org.clojure/tools.nrepl "0.2.10"]]}
             :test [:dev]
             :uberjar [:prod
                       {:aot :all
                        :hooks [leiningen.cljsbuild
                                leiningen.hooks.lesscss]}]})
