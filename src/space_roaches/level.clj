(ns space-roaches.level
  (:use [pallet.thread-expr])
  (:require [brute.entity :as e]
            [ripple.components :as c]
            [ripple.vector :as vector]
            [ripple.subsystem :as s]))

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

(s/defsubsystem level-systems
  :component-defs ['Mover])
