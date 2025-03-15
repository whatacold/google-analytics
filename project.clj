(defproject org.clojars.kenhuang/google-analytics "0.1.0-SNAPSHOT"
  :description "A ClojureScript lib to collect events to Google Analytics"
  :url "https://github.com/whatacold/google-analytics"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/core.async "1.7.701"]
                 [cljs-http "0.1.48"]
                 [org.clojure/clojure "1.12.0"]
                 [org.clojure/clojurescript "1.11.132"]]
  :repl-options {:init-ns google-analytics.core})
