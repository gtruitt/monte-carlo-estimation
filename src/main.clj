(ns main
  (:require
   [incanter.core :as core]
   [incanter.charts :as charts]
   [incanter.distributions :as dist]
   [clojure.string :as str]))

(def epic-revision-scale-mean 1.31)
(def epic-revision-scale-stdev 0.70)

(def cards-per-week-mean 12.62)
(def cards-per-week-stdev 6.93)

(def must-have-epic-card-counts [4 7 1 8 4 10])
(def nice-to-have-epic-card-counts [16 9 1 1 1 2 2 2 2])
(def just-pl-epic-card-counts [1 8 4 10 2 2 2 2])
(def all-epic-card-counts [4 7 16 9 1 1 1 1 8 4 10 2 2 2 2])

;; why to use gamma distributions
;; https://math.stackexchange.com/a/1335872

;; relates mean and stdev to gamma distributions
;; https://math.stackexchange.com/a/1810274

;; shows mean and stdev for a gamma distribution,
;; given its shape and scale; useful for verifying the math below
;; https://homepage.divms.uiowa.edu/~mbognar/applets/gamma.html

(defn square
  [n]
  (Math/pow n 2))

(defn gamma-shape
  [mean stdev]
  (/ (square mean) (square stdev)))

(defn gamma-scale
  [mean stdev]
  (/ 1 (/ mean (square stdev))))

(defn gamma-dist
  [mean stdev]
  (dist/gamma-distribution (gamma-shape mean stdev)
                           (gamma-scale mean stdev)))

(def epic-revision-scale-dist
  (gamma-dist epic-revision-scale-mean epic-revision-scale-stdev))

(def cards-per-week-dist
  (dist/normal-distribution cards-per-week-mean cards-per-week-stdev))

(defn rand-epic-revision-scale
  []
  (dist/draw epic-revision-scale-dist))

(defn rand-cards-per-week
  []
  (Math/round (dist/draw cards-per-week-dist)))

(defn estimated-epic-card-count
  [card-count]
  (+ card-count (* card-count (rand-epic-revision-scale))))

(defn weeks-to-complete
  [epic-card-counts]
  (let [est-epic-card-counts (map estimated-epic-card-count epic-card-counts)]
    (loop [weeks 0
           cards (reduce + est-epic-card-counts)]
      (if (<= cards 0)
        weeks
        (recur (inc weeks) (- cards (rand-cards-per-week)))))))

(defn predictions-to-file
  [prediction-count epic-card-counts file-name]
  (let [predictions
        (repeatedly #(weeks-to-complete epic-card-counts))]
    (spit file-name
          (str/join "\n" (take prediction-count predictions)))))

(comment
  [(gamma-shape epic-revision-scale-mean epic-revision-scale-stdev)
   (gamma-scale epic-revision-scale-mean epic-revision-scale-stdev)]

  [(gamma-shape 1 0.5)
   (gamma-scale 1 0.5)]

  (take 10 (repeatedly rand-epic-revision-scale))

  (take 10 (repeatedly rand-cards-per-week))

  (take 100 (repeatedly #(weeks-to-complete must-have-epic-card-counts)))

  (predictions-to-file 100000
                       must-have-epic-card-counts
                       "predictions-must-have.txt")

  (predictions-to-file 100000
                       nice-to-have-epic-card-counts
                       "predictions-nice-to-have.txt")

  (predictions-to-file 100000
                       just-pl-epic-card-counts
                       "predictions-just-pl.txt")

  (predictions-to-file 100000
                       all-epic-card-counts
                       "predictions-all.txt")

  (core/view
   (charts/histogram
    (take 10000 (repeatedly #(weeks-to-complete must-have-epic-card-counts)))
    :x-label "Number of Weeks"
    :nbins 15)))
