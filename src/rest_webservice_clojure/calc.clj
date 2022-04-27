(ns rest-webservice-clojure.calc
  (:gen-class))

(defn possibleInt? [value]
  (if-not (number? value)
    false
    (= (mod (float value) 1) 0.0)))

(defn filterInt [vec]
  (map int (filter possibleInt?
                   (filter number? vec))))

(defn addUp [vec]
  (when-not (vector? vec)
    (throw (Exception. "ERR_ADDUP_PARAM")))
  (reduce + (filterInt vec)))

(defn digitSum [value]
  (when-not (possibleInt? value)
    (throw (Exception. "ERR_DIGIT_SUM")))
  (reduce + (map #(- (int %) (int \0))
                 (str (Math/abs (int value))))))
