(defproject dynamo_utils "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [amazonica "0.3.132"]
                 [inflections "0.13.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
