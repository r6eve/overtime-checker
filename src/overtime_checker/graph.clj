(ns overtime-checker.graph
  (:require [clojure.java.io :as io]
            [clj-time.core :as t]
            [overtime-checker.core :as oc]))

(defn- m->h [m]
  (double (/ m 60)))

(defn- overs-by-day-of-week [overs]
  (into (sorted-map)
        (map (fn [[n times]] [n (m->h (apply + (map :over-m times)))]))
        (group-by (comp t/day-of-week :date) overs)))

#_(

(require '[clojisr.v1.r :refer [r r+]]
         '[clojisr.v1.require :refer [require-r]]
         '[clojisr.v1.applications.plotting :refer [plot->file]]
         '[tech.v3.dataset :as dataset])
(require-r '[ggplot2 :as gg])

(def nov (oc/overtime (io/resource "202011.tsv")))
(def dec (oc/overtime (io/resource "202012.tsv")))
(def jan (oc/overtime (io/resource "202101.tsv")))

(plot->file "month.svg"
            (let [x ["Nov." "Dec." "Jan."]
                  y (map :variable-hour [nov dec jan])]
              (r+ (gg/ggplot (dataset/->dataset {:x x :y y})
                             (gg/aes :x x :y y :group 1))
                  (gg/geom_line)
                  (gg/scale_x_discrete :limits x)
                  (gg/scale_y_continuous :limits [0, 'NA])
                  (gg/xlab "month")
                  (gg/ylab "over hours"))))

(plot->file "day-of-week.svg"
            (let [x ["Mon." "Tue." "Wed." "Thu." "Fri."]
                  y (map val (overs-by-day-of-week (:overs nov)))]
              (r+ (gg/ggplot (dataset/->dataset {:x x :y y})
                             (gg/aes :x x :y y))
                  (gg/geom_bar :stat "Identity")
                  (gg/scale_x_discrete :limits x)
                  (gg/xlab "day of week")
                  (gg/ylab "over hours"))))

)
