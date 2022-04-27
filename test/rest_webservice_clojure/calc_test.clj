(ns rest-webservice-clojure.calc-test
  (:require [clojure.test :refer [are deftest testing]]
            [rest-webservice-clojure.calc :refer [addUp digitSum]])
  (:gen-class))

(deftest addUp-test
  (testing "test valid parameters"
    (are [para result] (= result (addUp para))
      [] 0
      [0] 0
      [1 2 3] 6
      [-1] -1
      [-1,-2,3] 0
      ["1"] 0
      ["-1"] 0
      ["1", 1] 1
      ["-1", 1] 1
      [1.5] 0
      [-1.5] 0
      [1.5, 1] 1
      [-1.5, 1] 1
      [1.0] 1
      [-1.0] -1
      [100,-200.1,300,-40000,50,-60000,70,-800,0.001] -100280))

  (testing "test invalid parameters"
    (are [para] (thrown-with-msg? java.lang.Exception #"^ERR_ADDUP_PARAM$" (addUp para))
      nil
      0
      "invalid"
      {:value 1}
      "[]"
      "[0]")))

(deftest digitSum-test
  (testing "test valid parameters"
    (are [para result] (= result (digitSum para))
      0 0
      1 1
      -0 0
      -1 1
      123 6
      123456789 45
      2.0 2
      2.00 2))

  (testing "test invalid parameters"
    (are [para] (thrown-with-msg? java.lang.Exception #"^ERR_DIGIT_SUM$" (digitSum para))
      nil
      "invalid"
      "0"
      1.5
      [1]
      {:value 1})))
