(defproject braid "0.0.1"
  :source-paths ["src"]

  :main braid.core

  :plugins [[lein-environ "1.0.0"]
            [lein-tools-deps "0.4.5"]
            [lein-cljsbuild "1.1.8" :exclusions [org.clojure/clojure]]
            [lein-doo "0.1.7"]]

  :clean-targets ^{:protect false}
  ["resources/public/js"]

  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]

  :lein-tools-deps/config {:config-files [:install :user :project]}

  :figwheel {:server-port 3559}

  :cljsbuild {:test-commands {"once" ["lein" "doo" "phantom" "desktop-test" "once"]
                              "auto" ["lein" "doo" "phantom" "desktop-test" "auto"]}
              :builds
              [{:id "desktop-dev"
                :figwheel {:on-jsload "braid.core.client.desktop.core/reload"}
                :source-paths ["src/braid"]
                :compiler {:main braid.core.client.desktop.core
                           ;; uncomment to enable re-frame-10x (event debugger)
                           ;; will also need to uncomment below, under dev dependencies
                           ;; :preloads [day8.re-frame-10x.preload]
                           :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                           :asset-path "/js/dev/desktop/"
                           :output-to "resources/public/js/dev/desktop.js"
                           :output-dir "resources/public/js/dev/desktop/"
                           :optimizations :none
                           :verbose false}}

               {:id "desktop-test"
                :source-paths ["src/braid"
                               "test/braid/test/client"]
                :compiler {:main braid.test.client.runners.doo
                           :optimizations :none
                           :output-to "resources/public/js/desktop/tests/out/all-tests.js"
                           :output-dir "resources/public/js/desktop/tests/out"}}

               {:id "gateway-dev"
                :figwheel {:on-jsload "braid.core.client.gateway.core/reload"}
                :source-paths ["src/braid"]
                :compiler {:main braid.core.client.gateway.core
                           :asset-path "/js/dev/gateway/"
                           :output-to "resources/public/js/dev/gateway.js"
                           :output-dir "resources/public/js/dev/gateway"
                           :verbose true}}

               {:id "release"
                :source-paths ["src/braid"]
                :compiler {:asset-path "/js/prod/"
                           :output-dir "resources/public/js/prod/out"
                           :optimizations :advanced
                           :pretty-print false
                           :elide-asserts true
                           :closure-defines {goog.DEBUG false}
                           :modules {:cljs-base
                                     {:output-to "resources/public/js/prod/base.js"}
                                     :desktop
                                     {:output-to "resources/public/js/prod/desktop.js"
                                      :entries #{"braid.core.client.desktop.core"}}
                                     :gateway
                                     {:output-to "resources/public/js/prod/gateway.js"
                                      :entries #{"braid.core.client.gateway.core"}}}
                           :verbose true}}]}

  :min-lein-version "2.5.0"

  :profiles {:datomic-free
             {:dependencies [[com.datomic/datomic-free "0.9.5697"
                              :exclusions [joda-time
                                           com.google.guava/guava
                                           org.slf4j/slf4j-api]]]}
             :datomic-pro
             {:dependencies [[com.datomic/datomic-pro "0.9.5697"
                              :exclusions [joda-time
                                           com.google.guava/guava]]
                             [org.postgresql/postgresql "9.3-1103-jdbc4"]]}

             :dev
             [:datomic-free
              {:source-paths ["src" "dev-src"]
               :global-vars {*assert* true}
               :repl-options {:timeout 120000
                              :init-ns braid.dev.core}
               :dependencies [[figwheel-sidecar "0.5.18"
                               :exclusions
                               [org.clojure/google-closure-library-third-party
                                com.google.javascript/closure-compiler]]
                              [com.bhauman/rebel-readline "0.1.2"]
                              ;; uncomment the below to use re-frame-10x
                              #_[day8.re-frame/re-frame-10x "0.7.0"]]}]

             :prod
             [:datomic-free
              {:global-vars {*assert* false}}]

             :cider
             [:dev
              {:dependencies [[cider/piggieback "0.3.10"]]
               :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
               :plugins [[cider/cider-nrepl "0.20.0"]
                         [refactor-nrepl "2.4.0"]]}]

             :test
             [:dev]

             :uberjar
             [:prod
              {:aot [braid.core]
               :prep-tasks ["compile" ["cljsbuild" "once" "release"]]}]})
