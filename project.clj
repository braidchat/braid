(defproject chat "0.0.1"
  :source-paths ["src"]

  :dependencies [;server
                 [org.clojure/clojure "1.7.0-RC2"]
                 [javax.servlet/servlet-api "2.5"]
                 [commons-codec "1.10"]
                 [org.postgresql/postgresql "9.3-1103-jdbc4"]
                 [http-kit "2.1.19"]
                 [ring/ring-defaults "0.1.5"]
                 [fogus/ring-edn "0.3.0"]
                 [compojure "1.4.0"]
                 [environ "1.0.0"]
                 [com.taoensso/sente "1.5.0"]
                 [com.taoensso/timbre "4.0.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.datomic/datomic-pro "0.9.5201" :exclusions [joda-time]]
                 [com.taoensso/carmine "2.11.1"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [crypto-password "0.1.3"]
                 ;client
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.omcljs/om "0.8.8"]
                 [org.clojars.leanpixel/cljs-utils "0.2.0"]
                 [prismatic/schema "0.4.3"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/sente "1.5.0"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 ]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :main chat.server.handler
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.0.6"]
            [jamesnvc/lein-lesscss "1.4.0"] ]

  :clean-targets ^{:protect false} ["resources/public/js/out"]
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
                           :pretty-print false }}]}

  :min-lein-version "2.5.0"

  :profiles {:uberjar {:aot :all}})
