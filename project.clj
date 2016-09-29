(defproject braid "0.0.1"
  :source-paths ["src"]

  :dependencies [;server
                 [org.clojure/clojure "1.8.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [commons-codec "1.10"]
                 [commons-validator "1.5.1"]
                 [http-kit "2.2.0"]
                 [ring/ring-defaults "0.2.1"]
                 [fogus/ring-edn "0.3.0"]
                 [ring-cors "0.1.8"]
                 [compojure "1.5.1"]
                 [environ "1.0.3"]
                 [com.taoensso/timbre "4.5.1" :exclusions [org.clojure/tools.reader]]
                 [crypto-password "0.2.0"]
                 [clj-time "0.12.0"]
                 [instaparse "1.4.2"]
                 [com.taoensso/carmine "2.13.1"]
                 [clj-aws-s3 "0.3.10" :exclusions [joda-time org.apache.httpcomponents/httpclient com.fasterxml.jackson.core/jackson-core]]
                 [image-resizer "0.1.9"]
                 [clojurewerkz/quartzite "2.0.0"]
                 [inliner "0.1.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [mount "0.1.10"]
                 [com.cognitect/transit-clj "0.8.288" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [ring-transit "0.1.6" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 ;client
                 [org.clojure/clojurescript "1.9.92"]
                 [org.clojars.leanpixel/cljs-utils "0.4.2"]
                 [cljs-ajax "0.5.8"]
                 [secretary "1.2.3"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [clj-fuzzy "0.3.2"]
                 [reagent "0.6.0-rc"]
                 [re-frame "0.8.0"]
                 [cljsjs/husl "6.0.1-0"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 ;shared
                 [org.clojure/tools.reader "1.0.0-beta3"]
                 [org.clojure/core.async "0.2.385" :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/truss "1.3.3"]
                 [com.taoensso/sente "1.9.0-beta3" :exclusions [org.clojure/tools.reader taoensso.timbre]]
                 [prismatic/schema "1.1.2"]

                 ;mobile
                 [garden "1.3.2"]]

  :main braid.server.handler
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.1.3" :exclusions [org.clojure/clojure]]
            [lein-figwheel "0.5.4-7"]]

  :repl-options {:timeout 120000}
  :clean-targets ^{:protect false}
  ["resources/public/js"]

  :figwheel-options {:server-port 3559}

  :cljsbuild {:builds
              [
               {:id "desktop-dev"
                :figwheel {:on-jsload "braid.client.desktop.core/reload"}
                :source-paths ["src/braid/client"
                               "src/braid/common"]
                :compiler {:main braid.client.desktop.core
                           :asset-path "/js/desktop/out"
                           :output-to "resources/public/js/desktop/out/braid.js"
                           :output-dir "resources/public/js/desktop/out"
                           :verbose true}}

               {:id "desktop-release"
                :source-paths ["src/braid/client"
                               "src/braid/common"]
                :compiler {:main braid.client.desktop.core
                           :asset-path "/js/desktop/out"
                           :output-to "resources/public/js/desktop/out/braid.js"
                           :output-dir "resources/public/js/desktop/out_prod"
                           :optimizations :advanced
                           :pretty-print false}}

               {:id "mobile-dev"
                :figwheel {:on-jsload "braid.client.mobile.core/reload"
                           :websocket-host "192.168.1.157"}
                :source-paths ["src/braid/client"
                               "src/retouch"]
                :compiler {:main braid.client.mobile.core
                           :asset-path "/js/mobile/out"
                           :output-to "resources/public/js/mobile/out/braid.js"
                           :output-dir "resources/public/js/mobile/out"
                           :verbose true}}

               {:id "mobile-release"
                :source-paths ["src/braid/client"
                               "src/retouch"]
                :compiler {:main braid.client.mobile.core
                           :asset-path "/js/mobile/out"
                           :output-to "resources/public/js/mobile/out/braid.js"
                           :output-dir "resources/public/js/mobile/out_prod"
                           :optimizations :advanced
                           :pretty-print false}}]}

  :min-lein-version "2.5.0"

  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.9.5201"
                                   :exclusions [joda-time
                                                com.amazonaws/aws-java-sdk
                                                com.google.guava/guava
                                                org.slf4j/slf4j-api]]
                                  [figwheel-sidecar "0.5.4-7"
                                   :exclusions
                                   [org.clojure/google-closure-library-third-party
                                    com.google.javascript/closure-compiler]]]}
             :prod {:dependencies [[com.datomic/datomic-pro "0.9.5201"
                                    :exclusions [joda-time
                                                 com.amazonaws/aws-java-sdk
                                                 com.google.guava/guava]]
                                   [org.postgresql/postgresql "9.3-1103-jdbc4"]
                                   [org.clojure/tools.nrepl "0.2.12"]]}
             :test [:dev]
             :uberjar [:prod
                       {:aot :all
                        :prep-tasks ["compile" ["cljsbuild" "once" "desktop-release" "mobile-release"]]}]})
