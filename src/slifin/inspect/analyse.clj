(ns slifin.inspect.analyse
  (:require [slifin.inspect.diagnostics :as diagnostics]
            [clojure.string :refer [split-lines index-of]]))

(defn find-offsets [haystack needle]
  (let [find-offset (partial index-of haystack needle)]
    (take-while some? (iterate #(find-offset (inc %)) (find-offset 0)))))

(defn line->diagnostic [line col]
  (map #(diagnostics/create [col %] [col (+ % 6)] "oh noes array()")
    (find-offsets line "array(")))

(defn diagnostics [text uri]
  (let [lines (split-lines text)
        indexes (range (count lines))]
    (vec (mapcat line->diagnostic lines indexes))))
