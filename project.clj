(defproject chat "0.0.1"
  :source-paths ["src"]

  :dependencies [;server
                 [org.clojure/clojure "1.7.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [commons-codec "1.10"]
                 [http-kit "2.1.19"]
                 [ring/ring-defaults "0.2.0"]
                 [fogus/ring-edn "0.3.0"]
                 [ring-cors "0.1.7"]
                 [compojure "1.5.0"]
                 [environ "1.0.2"]
                 [com.taoensso/timbre "4.3.1" :exclusions [org.clojure/tools.reader]]
                 [crypto-password "0.2.0"]
                 [joda-time "2.9.2"]
                 [instaparse "1.4.1"]
                 [com.taoensso/carmine "2.12.2"]
                 [clj-aws-s3 "0.3.10" :exclusions [joda-time]]
                 [image-resizer "0.1.9"]
                 [clojurewerkz/quartzite "2.0.0"]
                 [inliner "0.1.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 ;client
                 [org.clojure/clojurescript "1.8.34"]
                 [org.omcljs/om "0.9.0"]
                 [org.clojars.leanpixel/cljs-utils "0.4.2"]
                 [secretary "1.2.3"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [org.clojars.leanpixel/om-fields "1.9.0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [clj-fuzzy "0.3.1"]
                 ;shared
                 [org.clojure/tools.reader "1.0.0-alpha3"]
                 [org.clojure/core.async "0.2.374" :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/sente "1.8.1" :exclusions [org.clojure/tools.reader]]

                 ;mobile
                 [re-frame "0.7.0"]
                 [garden "1.3.2"]]

  :main chat.server.handler
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.1"]]

  :repl-options {:timeout 120000}
  :clean-targets ^{:protect false}
  ["resources/public/js"]

  :figwheel-options {:server-port 3559}

  :cljsbuild {:builds
              [
               {:id "desktop-dev"
                :figwheel true
                :source-paths ["src/chat/client"
                               "src/chat/shared"
                               "src/braid/ui"
                               "src/braid/common"]
                :compiler {:main chat.client.core
                           :asset-path "/js/desktop/out"
                           :output-to "resources/public/js/desktop/out/braid.js"
                           :output-dir "resources/public/js/desktop/out"
                           :verbose true}}

               {:id "desktop-release"
                :source-paths ["src/chat/client"
                               "src/chat/shared"
                               "src/braid/ui"
                               "src/braid/common"]
                :compiler {:main chat.client.core
                           :asset-path "/js/desktop/out"
                           :output-to "resources/public/js/desktop/out/braid.js"
                           :output-dir "resources/public/js/desktop/out"
                           :optimizations :advanced
                           :pretty-print false }}

               {:id "mobile-dev"
                :figwheel true
                :source-paths ["src/braid/mobile"
                               "src/braid/ui"
                               "src/retouch"]
                :compiler {:main braid.mobile.core
                           :asset-path "/js/mobile/out"
                           :output-to "resources/public/js/mobile/out/braid.js"
                           :output-dir "resources/public/js/mobile/out"
                           :verbose true}}]}

  :min-lein-version "2.5.0"

  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.9.5201" :exclusions [joda-time com.amazonaws/aws-java-sdk]]
                                  [figwheel-sidecar "0.5.0-6" :exclusions
                                   [org.clojure/google-closure-library-third-party
                                    com.google.javascript/closure-compiler]]]}
             :prod {:dependencies [[com.datomic/datomic-pro "0.9.5201" :exclusions [joda-time com.amazonaws/aws-java-sdk]]
                                   [org.postgresql/postgresql "9.3-1103-jdbc4"]
                                   [org.clojure/tools.nrepl "0.2.12"]]}
             :test [:dev]
             :uberjar [:prod
                       {:aot :all
                        :prep-tasks ["compile" ["cljsbuild" "once" "desktop-release"]]}]})
