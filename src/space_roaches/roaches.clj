(ns space-roaches.roaches
  (:use [pallet.thread-expr])
  (import [com.badlogic.gdx.physics.box2d RayCastCallback])
  (:require [brute.entity :as e]
            [space-roaches.level :as level]
            [ripple.vector :as vector]
            [ripple.components :as c]
            [ripple.rendering :as r]
            [ripple.sprites :as sprites]
            [ripple.prefab :as prefab]
            [ripple.assets :as a]
            [ripple.transform :as transform]
            [ripple.physics :as physics]
            [ripple.subsystem :as s]))

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

(defn- disable-collision
  "Disable collision by switching to a different category, and setting mask to 0 (collide with nothing)"
  [body]
 (let [new-filter (physics/create-filter :category 2 :mask 0)]
    (doseq [fixture (.getFixtureList body)]
      (.setFilterData fixture new-filter))))

(defn- spawn-gibs
  [system entity]
  (let [gib-prefabs (:gib-prefabs (e/get-component system entity 'SpaceRoach))
        [x y] (:position (e/get-component system entity 'Transform))]
    (-> system
        (for-> [gib gib-prefabs]
               (prefab/instantiate gib {:physicsbody {:x x
                                                      :y y
                                                      :angular-velocity (* 5 (- 1 (rand 2)))
                                                      :velocity-x (* 3 (- 1 (rand 2)))
                                                      :velocity-y (* 3 (- 1 (rand 2)))}})))))

(defn- kill-roach
  [system entity]
  (doto (:body (e/get-component system entity 'PhysicsBody))
    (disable-collision))
  (-> system
      (spawn-gibs entity)
      (level/notify-destroyed entity)

      ;(e/update-component entity 'SpriteRenderer #(assoc % :enabled false))
      ;(e/update-component entity 'SpaceRoach #(assoc % :dead true))

      ;; Just destroy the entity for now...
      (c/destroy-entity entity)))

(defn on-damaged
  [system entity event]
  (kill-roach system entity))

(c/defcomponent SpaceRoach
  :on-event [:take-damage on-damaged]
  :on-pre-render update-roach
  :fields [:speed {:default 1.0}
           :gib-prefabs {:default []}])

(s/defsubsystem roaches
  :component-defs ['SpaceRoach])
