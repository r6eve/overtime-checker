(ns overtime-checker.core
  (:refer-clojure :exclude [time type])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(defn- m->h [m]
  (double (/ m 60)))

(defn- h->m [h]
  (* 60 h))

(defn- parse [date type time]
  {:date (f/parse (f/formatters :year-month-day) date)
   :type (if (= type "half") :half-hour :all-hour) ; XXX:
   :time (->> (str/split time #"[\s,]+") ; XXX: ambiguous
              (map #(str/split % #"-"))
              (map (fn [[start end]]
                     {:start (f/parse (f/formatters :hour-minute) start)
                      :end (f/parse (f/formatters :hour-minute) end)})))})

(defn- fix-end [start end]
  (if (t/before? start end)
    end
    (t/plus end (t/hours 24))))

(defn- over-m [{:keys [type time]} params]
  (when (#{:half-hour :all-hour} type)
    (let [o (transduce (map (fn [{:keys [start end]}]
                              (t/in-minutes
                               (t/interval start (fix-end start end)))))
                       +
                       time)
          e (h->m (params type))]
      (- o e))))

(defn- working-hours [overs]
  {:variable-hour (m->h (transduce (map :over-m) + overs))
   :fixed-hour (m->h (transduce (keep (fn [{:keys [over-m]}]
                                        (when (pos? over-m) over-m)))
                                +
                                overs))
   :overs overs})

(defn overtime
  ([tsv-path] (overtime tsv-path {:half-hour 4 :all-hour 8}))
  ([tsv-path params]
   (with-open [r (io/reader tsv-path)]
     (->> r
          line-seq
          rest ; remove header
          (sequence (comp (remove #(str/starts-with? % "#"))
                          (map #(str/split % #"\t"))
                          (map (partial apply parse))
                          (map (fn [m] (assoc m :over-m (over-m m params))))))
          working-hours
          doall))))

#_(
(def nov (overtime (io/resource "202011.tsv")))
(:variable-hour nov) ;; => 10.0
(:fixed-hour nov) ;; => 11.0
)
