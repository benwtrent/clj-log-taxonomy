(ns log-taxonomy-clj.common)

(def wild-card "<*>")
(def space (re-pattern " "))
(defn wild-card? [^String s]
  (= s wild-card))

(defn r-some-pattern [patterns x]
  (some #(re-matches % x) patterns))

(defn close? [^double lft ^double rgt]
  (< (Math/abs (- lft rgt)) 1e-9))

(defn sum [coll]
  (reduce + coll))

(defn mean [coll]
  (double (/ (sum coll) (count coll))))