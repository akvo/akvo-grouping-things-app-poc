(defproject myapp "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[baking-soda "0.2.0" :exclusions [cljsjs/react-bootstrap]]
                 [clj-oauth "1.5.5"]
                 [cljs-ajax "0.7.5"]
                 [cljsjs/react-popper "0.10.4-0"]
                 [cljsjs/react-transition-group "2.4.0-0"]
                 [clojure.java-time "0.3.2"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.fasterxml.jackson.core/jackson-core "2.9.7"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jdk8 "2.9.7"]
                 [compojure "1.6.1"]
                 [conman "0.8.3"]
                 [cprop "0.1.13"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [funcool/struct "1.3.0"]
                 [luminus-immutant "0.2.4"]
                 [luminus-migrations "0.6.1"]
                 [luminus-transit "0.1.1"]
                 [luminus/ring-ttl-session "0.3.2"]
                 [markdown-clj "1.0.5"]
                 [metosin/muuntaja "0.6.1"]
                 [metosin/ring-http-response "0.9.0"]
                 [mount "0.1.14"]
                 [nrepl "0.4.5"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.439" :scope "provided"]
                 [org.clojure/tools.cli "0.4.1"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.postgresql/postgresql "42.2.5"]
                 [org.webjars.bower/tether "1.4.4"]
                 [org.webjars/bootstrap "4.1.3"]
                 [org.webjars/font-awesome "5.5.0"]
                 [org.webjars/webjars-locator "0.34"]
                 [re-frame "0.10.6"]
                 [reagent "0.8.1"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [secretary "1.2.3"]
                 [selmer "1.12.3"]
                 ;; GAE SDK
                 [org.akvo/commons "0.4.2" :exclusions [[org.clojure/tools.reader]]]
                 [com.google.appengine/appengine-tools-sdk "1.9.53"]
                 [com.google.appengine/appengine-remote-api "1.9.53"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.9.53"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot myapp.core

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-immutant "2.1.0"]]
  :clean-targets ^{:protect false}
  [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :figwheel
  {:http-server-root "public"
   :server-logfile "log/figwheel-logfile.log"
   :server-port 3449
   :nrepl-host "0.0.0.0"
   :nrepl-port 7889
   :css-dirs ["resources/public/css"]
   :hawk-options {:watcher :polling}
   :nrepl-middleware
   #_[cider/wrap-cljs-repl cider.piggieback/wrap-cljs-repl]
   [cider.piggieback/wrap-cljs-repl]
   }
  

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-dir "target/cljsbuild/public/js"
                 :output-to "target/cljsbuild/public/js/app.js"
                 :source-map "target/cljsbuild/public/js/app.js.map"
                 :optimizations :advanced
                 :pretty-print false
                 :infer-externs true
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["react/externs/react.js"]}}}}


             :aot :all
             :uberjar-name "myapp.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev [:project/dev :profiles/dev]
   :test [:project/dev :project/test :profiles/test]

   :project/dev {:jvm-opts ["-Dconf=env/dev/resources/config.edn"]
                 :dependencies [[binaryage/devtools "0.9.10"]
                                [cider/piggieback "0.3.10"]
                                [doo "0.1.10"]
                                [expound "0.7.1"]
                                [figwheel-sidecar "0.5.17"]
                                ;[pjstadig/humane-test-output "0.9.0"]
                                [prone "1.6.1"]
                                [re-frisk "0.5.4"]
                                [ring/ring-devel "1.7.1"]
                                [ring/ring-mock "0.3.2"]]
                 :plugins [
                           ;[com.jakemccrary/lein-test-refresh "0.23.0"]
                           [lein-doo "0.1.10"]
                           [lein-figwheel "0.5.17"]]
                 :cljsbuild
                 {:builds
                  {:app
                   {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                    :figwheel {:on-jsload "myapp.core/mount-components"}
                    :compiler
                    {:main "myapp.app"
                     :asset-path "/js/out"
                     :output-to "target/cljsbuild/public/js/app.js"
                     :output-dir "target/cljsbuild/public/js/out"
                     :source-map true
                     :infer-externs true
                     :parallel-build true
                     :optimizations :none
                     :pretty-print true
                     :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                     :foreign-libs [{:file "react-libs/dist/index_bundle.js"
                                     :provides ["sortable-tree"]
                                     :global-exports {react React
                                                      react-dom ReactDOM
                                                      sortable-tree SortableTree}}]
                     :preloads [re-frisk.preload]}}}}



                 :doo {:build "test"}
                 :source-paths ["env/dev/clj"]
                 :resource-paths ["env/dev/resources"]
                 :repl-options {:init-ns user}
                 ;:injections
                 ; [(require 'pjstadig.humane-test-output) (pjstadig.humane-test-output/activate!)]
                 }
   :project/test {:jvm-opts ["-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "myapp.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}

                  }
   :profiles/dev {}
   :profiles/test {}})
