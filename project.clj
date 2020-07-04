(defproject habit_tracker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [environ             "1.1.0"]
                 [morse               "0.4.3"]
                 [seancorfield/next.jdbc "1.1.547"]
                 [org.postgresql/postgresql "42.2.10"]
                 [honeysql "1.0.444"]]

  :plugins [[lein-environ "1.1.0"]]

  :main ^:skip-aot habit-tracker.core
  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
