(defproject log-taxonomy-clj "0.1.0-SNAPSHOT"
  :description "Collection of log categorization algorithms"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.4.1"]]
  :main ^:skip-aot log-taxonomy-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
