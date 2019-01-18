(ns origami.utils-test
  (:require
    [opencv4.utils :as cvu]
    [clojure.test :refer :all]
    [opencv4.core :refer :all]))

(deftest mat->image
	(is (not (nil?
	(-> (new-mat 3 3 CV_8UC1)
		(set-to!  (new-scalar 10 10 10))
		(cvu/mat->image))))))