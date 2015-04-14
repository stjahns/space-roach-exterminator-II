(ns space-roaches.core
  (:require [play-clj.core :refer :all]
            [ripple.repl :as repl]
            [ripple.assets :as a]
            [ripple.audio :as audio]
            [ripple.components :as c]
            [ripple.core :as ripple]
            [ripple.event :as event]
            [ripple.physics :as physics]
            [ripple.prefab :as prefab]
            [ripple.rendering :as rendering]
            [ripple.sprites :as sprites]
            [ripple.subsystem :as subsystem]
            [ripple.tiled-map :as tiled-map]
            [ripple.transform :as transform]
            [space-roaches.player :as player]
            [space-roaches.level :as level]
            [space-roaches.roaches :as roaches])
  (:import [com.badlogic.gdx ApplicationListener]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(declare shutdown restart)

(def subsystems [transform/transform
                 event/events
                 rendering/rendering
                 physics/physics
                 prefab/prefabs
                 sprites/sprites
                 audio/audio
                 tiled-map/level
                 player/player
                 roaches/roaches
                 level/level-systems])

(def asset-sources ["space_roaches/assets.yaml"])

(defn on-initialized
  "Load the PlatformLevel"
  [system]
  (-> system
      (prefab/instantiate "ShipLevel" {})))

(defscreen main-screen
  :on-show
  (fn [screen entities]

    ;; Initialize Ripple
    (reset! ripple/sys (-> (ripple/initialize subsystems asset-sources on-initialized)
                           (assoc-in [:renderer :clear-color] [0.2 0.2 0.2 1.0])))

    ;; Use an orthographic camera
    (update! screen :renderer (stage) :camera (orthographic))

    nil)

  :on-touch-down
  (fn [screen entities]
    (reset! ripple/sys (-> @ripple/sys (subsystem/on-system-event :on-touch-down)))
    nil)

  :on-render
  (fn [screen entities]
    (reset! ripple/sys (-> @ripple/sys
                    (subsystem/on-system-event :on-pre-render)
                    (subsystem/on-system-event :on-render)))
    (when (:restart @ripple/sys)
      (shutdown)
      (restart))
    nil)

  :on-resize
  (fn [screen entities]
    (reset! ripple/sys (-> @ripple/sys (subsystem/on-system-event :on-resize)))
    nil))

(defgame space-roaches
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(defn -main []
  (LwjglApplication. space-roaches "Space Roach Exterminator II" 800 600)
  (Keyboard/enableRepeatEvents true))

;; For development ...

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defn shutdown []
  (set-screen! space-roaches blank-screen)
  (Thread/sleep 100)
  (subsystem/on-system-event @ripple/sys :on-shutdown))

(defn restart []
  (set-screen! space-roaches main-screen))

;; For exception handling...
(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! space-roaches blank-screen)))))

(defn reload-all []
  (shutdown)
  (on-gl (set-screen! space-roaches main-screen)))

(defn reload-and-require-all []
  (shutdown)

  (println "Recompiling...")

  (require 'space-roaches.core :reload-all)

  (println "Reloading...")

  (on-gl (set-screen! space-roaches main-screen)))

(defn rra [] (reload-and-require-all))
(defn ra [] (reload-all))

