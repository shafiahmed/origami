(ns opencv4.colors.html
   (:require [opencv4.core :only [new-scalar]]))

(defn- hex->num [#^String s]
   (Integer/parseInt s 16))

(defn ->scalar [#^String s]
  (let [ [r g b] (map #(apply str %) (partition 2 (.substring s 1))) ]
  (opencv4.core/new-scalar (hex->num b) (hex->num g) (hex->num r))))
