(ns log-taxonomy-clj.drain
  (:gen-class)
  (:use log-taxonomy-clj.common)
  (:require [clojure.string :as str]))

(defn pre-process-line [^String line patterns]
  (println patterns)
  (let [tokens (-> line
                   (str/trim)
                   (str/split space))]
    (map #(if (r-some-pattern patterns %)
            "<*>"
            (str/replace % #"\d+" "<*>")) tokens)))

(defn similarity [log-group log]
  (if (nil? log-group)
    nil
    (let [log-pattern (:pattern log-group)
          similarities (map #(cond
                               (= %1 %2) [1 1]
                               (wild-card? %1) [0 1]
                               :default [0 0]) log-pattern log)]
      {:exact   (double (->> similarities
                             (map first)
                             (mean)))
       :similar (double (->> similarities
                             (map second)
                             (mean)))})))

(defn new-pattern [pattern log]
  (if (empty? pattern)
    log
    (map #(if (= %1 %2) %1 wild-card) pattern log)))

(defn add-log [log-group log]
  (assoc log-group :pattern
                   (new-pattern (:pattern log-group) log)))

(defn best-group [log-groups log]
  (if (empty? log-groups)
    nil
    (loop [best-log-group (first log-groups)
           pos 0
           best-pos 0
           best-similarity (similarity (first log-groups) log)
           log-groups (rest log-groups)]
      (if (empty? log-groups)
        {:log-group  best-log-group
         :pos        best-pos
         :similarity (:similar best-similarity)}
        (let [cur-group (first log-groups)
              cur-sim (similarity cur-group log)
              better? (or (> (:similar cur-sim) (:similar best-similarity))
                          (and (close? (:similar cur-sim) (:similar best-similarity))
                               (> (:exact cur-sim) (:exact best-similarity))))
              best-pos (if better? pos best-pos)
              best-log-group (if better? cur-group best-log-group)
              best-similarity (if better? cur-sim best-similarity)]
          (recur best-log-group (inc pos) best-pos best-similarity (rest log-groups)))))))

(defn update-log-groups [child log similarity-threshold]
  (let [log-groups (or (:log-groups child) [])
        best-log-group (best-group log-groups log)
        new-groups (if (or (nil? best-log-group) (< (:similarity best-log-group) similarity-threshold))
                     (conj log-groups {:pattern log})
                     (assoc log-groups
                       (:pos best-log-group)
                       (add-log (:log-group best-log-group) log)))]
    (assoc child :log-groups new-groups)))

(defn add-log-to-tree [tree log max-layers similarity-threshold]
  (let [path (take max-layers log)
        first-child (or (get tree (count log)) {})
        child (update-in first-child path #(update-log-groups % log similarity-threshold))]
    (assoc tree (count log) child)))

(defn drain
  ([lines] (drain lines []))
  ([lines filters] (drain lines filters 25))
  ([lines filters max-layers] (drain lines filters max-layers 0.75))
  ([lines filters max-layers similarity-threshold] (drain lines filters max-layers similarity-threshold {}))
  ([lines filters max-layers similarity-threshold tree]
   (reduce (fn [t l]
             (add-log-to-tree t (pre-process-line l filters) max-layers similarity-threshold)) tree lines)))
