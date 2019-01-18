(ns opencv4.gorilla
  (:import [java.awt.image BufferedImage])
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.string :as string]
            [opencv4.utils :refer [mat->image image->bytes]]
            [gorilla-renderable.core :as render]))
 
(defrecord ImageView [image alt type width height]
  render/Renderable
  (render [{:keys [image alt type width height]}]
      {:type :html
       :content (str "<img src=\"data:image/\"" 
                    type 
                    ";base64," 
                    (String. (b64/encode (image->bytes image type width height))) 
                    "\" alt=\"" alt "\"" 
                    "width=\"" width "\" height=\"" height "\">")
       :value (pr-str image)}))

;
; image here is of type: [java.awt.image BufferedImage]
; but should be possible to use a mat directly !
(defn- image-view [^BufferedImage image & {:keys [alt type width height]}]
  (let [alt (or alt "")
        type (string/lower-case (or type "png"))
        iw (.getWidth image)
        ih (.getHeight image)
        [w h] (cond
               (and width height) [(int width) (int height)]
               width [(int width) (int (* (/ width iw) ih))]
               height [(int (* (/ height ih) iw)) (int height)]
               :else [iw ih])]
    (ImageView. image alt type w h)))

(defn ->view[mat]
  (image-view (mat->image mat)))