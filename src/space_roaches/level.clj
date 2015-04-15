(ns space-roaches.level
  (:use [pallet.thread-expr])
  (:require [brute.entity :as e]
            [ripple.components :as c]
            [ripple.vector :as vector]
            [ripple.prefab :as prefab]
            [ripple.event :as event]
            [ripple.subsystem :as s]))

;; Mover component
;; TODO - This general enough to move to ripple core

(defn- do-move
  [system entity target-position target-state]
  (let [mover (e/get-component system entity 'Mover)
        position (:position (e/get-component system entity 'Transform))
        elapsed-time (- (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.0) (:timer mover))]
    (if (>= elapsed-time (:transition-time mover))
      ;; Set to final position and update state
      (-> system
          (e/update-component entity 'Mover #(assoc % :state target-state))
          (e/update-component entity 'Transform #(assoc % :position target-position)))
      ;; Set to interpolated position
      (let [position (vector/lerp position
                                  target-position
                                  (/ elapsed-time (:transition-time mover))) ]
        (e/update-component system entity 'Transform #(assoc % :position position))))))

(defn- update-mover
  [system entity]
  (let [mover (e/get-component system entity 'Mover)]
    (case (:state mover)
      :at-position-a system
      :at-position-b system
      :moving-to-a (do-move system entity (:position-a mover) :at-position-a)
      :moving-to-b (do-move system entity (:position-b mover) :at-position-b))))

(defn- move-to-b
  [system entity event]
  (e/update-component system entity 'Mover #(assoc %
                                                   :state :moving-to-b
                                                   :timer (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.0))))

(defn- move-to-a
  [system entity event]
  (e/update-component system entity 'Mover #(assoc %
                                                   :state :moving-to-a
                                                   :timer (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.0))))

(defn- on-spawn
  "On spawn, set position-a to current position and position-b to position + offset"
  [system entity event]
  (let [mover (e/get-component system entity 'Mover)
        position (:position (e/get-component system entity 'Transform))]
    (-> system
        (e/update-component entity 'Mover #(assoc %
                                                  :position-a position
                                                  :position-b (vector/add position (:offset mover)))))))

(c/defcomponent Mover
  ;; Requires Transform
  ;; Requires EventHub
  :fields [:offset {:default [0.0 0.0]}
           :state {:default :at-position-a}
           :transition-time {:default 1.0}]
  :on-pre-render update-mover
  :on-event [:on-spawn on-spawn
             :move move-to-b
             :reset move-to-a])

;; Spawner component
;; Spawns an entity.

(defn- spawner-spawn-prefab
  [system entity]
  (let [spawner (e/get-component system entity 'Spawner)
        [x y] (:position (e/get-component system entity 'Transform))]
    (.play (:spawn-sound spawner))
    (-> system
        (e/update-component entity 'Spawner #(assoc % :spawn-timer nil))
        (prefab/instantiate (:prefab spawner) {:transform {:position [x y]}
                                               :physicsbody {:x x :y y}
                                               :spawnedentity {:spawner-id entity}}))))

(defn- spawner-on-spawn
  [system entity event]
  (spawner-spawn-prefab system entity))

(defn- spawner-on-entity-destroyed
  "Set spawn-timer to spawn after spawn-delay"
  [system entity event]
  (let [spawner (e/get-component system entity 'Spawner)]
    (e/update-component system entity 'Spawner #(assoc % :spawn-timer
                                                       (+ (com.badlogic.gdx.utils.TimeUtils/millis)
                                                          (* 1000 (:spawn-delay spawner)))))))

(defn- update-spawner
  [system entity]
  (let [spawner (e/get-component system entity 'Spawner)]
    (-> system
        (when-> (and (:spawn-timer spawner)
                     (< (:spawn-timer spawner) (com.badlogic.gdx.utils.TimeUtils/millis)))
                (spawner-spawn-prefab entity)))))

(c/defcomponent Spawner
  :fields [:spawn-delay {:default 5.0}
           :spawn-timer {:default nil}
           :spawn-sound {:asset true}
           :prefab nil]
  :on-pre-render update-spawner
  :on-event [:on-spawn spawner-on-spawn
             :on-entity-destroyed spawner-on-entity-destroyed])

(c/defcomponent SpawnedEntity
  :fields [:spawner-id nil])

(defn notify-destroyed
  [system entity]
  (let [spawner (:spawner-id (e/get-component system entity 'SpawnedEntity))]
    (event/send-event system spawner {:event-id :on-entity-destroyed})))

(s/defsubsystem level-systems
  :component-defs ['Mover, 'Spawner, 'SpawnedEntity])
