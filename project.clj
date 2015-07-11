(defproject chat "0.0.1"
  :source-paths ["src"]

  :plugins [[lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :profiles {:server {:dependencies [[org.clojure/clojure "1.7.0-RC2"]
                                     [javax.servlet/servlet-api "2.5"]
                                     [http-kit "2.1.18"]
                                     [fogus/ring-edn "0.3.0"]
                                     [compojure "1.3.4"]
                                     [environ "1.0.0"]
                                     [com.taoensso/sente "1.5.0"]
                                     [com.taoensso/timbre "4.0.2"]
                                     [org.clojure/core.async "0.1.346.0-17112a-alpha"]]}
             :client {:plugins [[lein-cljsbuild "1.0.6"]
                                [jamesnvc/lein-lesscss "1.4.0"]]
                      :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                                     [org.clojure/clojurescript "0.0-3308"]
                                     [org.omcljs/om "0.8.8"]
                                     [org.clojars.leanpixel/cljs-utils "0.2.0"]
                                     [prismatic/schema "0.4.3"]
                                     [secretary "1.2.3"]
                                     [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                                     [com.taoensso/sente "1.5.0"]]
                      :lesscss-paths ["resources/less"]
                      :lesscss-output-path "resources/public/css/out"
                      :cljsbuild {:builds
                                  [{:id "dev"
                                    :source-paths ["src/chat/client"]
                                    :compiler {:main chat.client.core
                                               :asset-path "/js/out"
                                               :output-to "resources/public/js/out/chat.js"
                                               :output-dir "resources/public/js/out"
                                               :optimizations :none
                                               :source-map true}}
                                   {:id "release"
                                    :source-paths ["src/chat/client"]
                                    :compiler {:main chat.client.core
                                               :output-to "resources/public/js/out/chat.js"
                                               :optimizations :advanced
                                               :pretty-print false }}]}}
             :common [:server :client]
             :dev-common {:repl-options {:init-ns chat.server.handler}}
             :dev-overrides {}
             :dev [:common :dev-common :dev-overrides]
             })
