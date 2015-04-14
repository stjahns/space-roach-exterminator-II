(ns space-roaches.core
  (:require [ripple.core]
            [ripple.repl]
            [space_roaches.player :as player])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(def config { :subsystems [player/player] ;; TODO declare built-in subsystems...
              :asset-sources "assets.yaml" })

(defn -main []

  ;; TODO - pass config into create-game
  (let [game (ripple.core/create-game)]
    (LwjglApplication. game "Space Roach Exterminator II" 800 600)
    (Keyboard/enableRepeatEvents true)))
