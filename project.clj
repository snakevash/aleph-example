(defproject aleph-example "0.1.0"
  :description "aleph 中文范例"
  :url "http://github.com/snakevash/aleph-example"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [aleph "0.4.3"]
                 [gloss "0.2.6"]
                 [compojure "1.5.2"]
                 [org.clojure/core.async "0.3.442"]]
  :main ^:skip-aot aleph-example.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
