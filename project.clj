(defproject braid "0.0.1"
  :source-paths ["src"]

  :main braid.core

  :plugins [[lein-environ "1.0.0"]
            [lein-tools-deps "0.4.5"]]

  :clean-targets ^{:protect false}
  ["resources/public/js"]

  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]

  :lein-tools-deps/config {:config-files [:install :user :project]}

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
                             [org.postgresql/postgresql "42.2.2"]]}

             :dev
             [:datomic-free
              {:source-paths ["src" "dev-src"]
               :global-vars {*assert* true}
               :repl-options {:timeout 120000
                              :init-ns braid.dev.core}
               :dependencies [[com.bhauman/figwheel-main "0.2.12"
                               :exclusions [org.clojure/clojurescript]]]}]

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
               :dependencies [[com.bhauman/figwheel-main "0.2.12"
                               :exclusions [org.clojure/clojurescript]]]
               :prep-tasks ["compile"
                            ["trampoline" "run" "-m" "figwheel.main" "-bo" "prod"]]}]})
