(ns space-roaches.roaches
  (:use [pallet.thread-expr])
  (import [com.badlogic.gdx.physics.box2d RayCastCallback])
  (:require [brute.entity :as e]
            [ripple.vector :as vector]
            [ripple.components :as c]
            [ripple.rendering :as r]
            [ripple.sprites :as sprites]
            [ripple.prefab :as prefab]
            [ripple.assets :as a]
            [ripple.transform :as transform]
            [ripple.subsystem :as s]))

;; + Roaches move towards player
;; + Player dies when roach touches
;; + Roaches die when shot

(defn get-player-entity
  "Get the player entity, assuming a single player"
  [system]
  (first (e/get-all-entities-with-component system 'Player)))

(defn can-see-player?
  "Check if player is alive, if so:
  Check if roach has line of sight to Player, by doing a RayCast from
  roach to player and seeing if it hits anything other than the player
  TODO - want to ignore certain layers, ie Triggers, other roaches, etc"
  [system entity]
  (let [player (get-player-entity system)
        player-transform (e/get-component system player 'Transform)
        player-pos (transform/get-position system player-transform)
        roach-transform (e/get-component system entity 'Transform)
        roach-pos (transform/get-position system roach-transform)
        can-see (atom true)]
    (if (not (:dead (e/get-component system player 'Player)))
      (do (.rayCast (get-in system [:physics :world])
                  (reify RayCastCallback
                    (reportRayFixture [this fixture point normal fraction]
                      (let [data (.getUserData fixture)]
                        (if (not (or (= (:entity data) player)
                                     (= (:entity data) entity)))
                          (do (reset! can-see false) 0.0) ;; set can-see to false, return 0.0 to end raycast
                          1.0))))
                  player-pos
                  roach-pos)
          @can-see)
      false)))

(defn move-towards-player
  [system entity]
  ;; TODO -- nedd delta-time
  (let [roach-pos (:position (e/get-component system entity 'Transform))
        speed (:speed (e/get-component system entity 'SpaceRoach))
        player (get-player-entity system)
        player-pos (:position (e/get-component system player 'Transform))
        roach-to-player (vector/normalized (vector/sub player-pos roach-pos))
        new-pos (vector/add roach-pos
                            (vector/scale roach-to-player speed))]
    (e/update-component system entity 'Transform #(assoc % :position new-pos))))

(defn update-roach
  [system entity]
  (-> system
      (when-> (can-see-player? system entity)
        (move-towards-player entity))))

(c/defcomponent SpaceRoach
  :on-pre-render update-roach
  :fields [:speed {:default 1.0}])

(s/defsubsystem roaches
  :component-defs ['SpaceRoach])

