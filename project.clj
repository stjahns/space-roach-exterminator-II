(defproject space_roaches "0.1.0-SNAPSHOT"
  :description "Space Roach Exterminator II"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.1.0"]
                 [ripple "0.1.0-SNAPSHOT"]]
  :main ^:skip-aot space-roaches.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
