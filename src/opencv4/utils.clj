
(ns opencv4.utils
  (:require
    [opencv4.core :as cv]
    [opencv4.video :as vid])
  (:import [org.opencv.core Size CvType Core Mat MatOfByte]
    [org.opencv.imgcodecs Imgcodecs]
    [org.opencv.videoio VideoCapture]
    [org.opencv.imgproc Imgproc]

    [java.net URL]
    [java.nio.channels ReadableByteChannel Channels]
    [java.io File FileOutputStream]

    [java.awt Image]
    [java.awt.image BufferedImage]
    [java.io ByteArrayOutputStream]

    [javax.imageio ImageIO]
    [javax.swing ImageIcon JFrame JLabel]
    [java.awt.event KeyListener MouseAdapter]
    [java.awt FlowLayout]))

;;;
; CLEAN
;;;
(defn clean-up-namespace[]
  (map #(ns-unmap *ns* %) (keys (ns-interns *ns*))))


;;;;
; BUFFERED IMAGE
;;;;

(defn mat-to-buffered-image [src]
  (let[ matOfBytes (MatOfByte.)]
  (Imgcodecs/imencode ".jpg" src matOfBytes)
  (ImageIO/read
    (java.io.ByteArrayInputStream. (.toArray matOfBytes)))))
(def mat->image mat-to-buffered-image)

(defn image->bytes [^Image image ^String type width height]
  (let [bi (BufferedImage. width height (if (#{"png" "gif"} type)
                                          BufferedImage/TYPE_INT_ARGB
                                          BufferedImage/TYPE_INT_RGB))
        baos (ByteArrayOutputStream.)]
    (doto (.getGraphics bi) (.drawImage image 0 0 width height nil))
    (ImageIO/write bi type baos)
    (.toByteArray baos)))

(defn buffered-image-to-mat[bi]
  (let [mat (Mat. (.getHeight bi) (.getWidth bi) (CvType/CV_8UC3))
  bytes
  (-> bi
    (.getRaster)
    (.getDataBuffer)
    (.getData)
    )]
    (.put mat 0 0 bytes)
    mat))
(def image->mat buffered-image-to-mat)

(defn image<-url [url]
  (let[ connection (->  url
  (java.net.URL.)
    (.openConnection))]
    (.setRequestProperty connection
      "User-Agent"
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31")
      (ImageIO/read (.getInputStream connection))))


;;;;
; LOAD UNLOAD
;;;;

(defn ->cv1 [bytes h w]
  (let [u (cv/new-mat h w cv/CV_8U)]
        (cv/>> u (byte-array bytes))))

(defn matrix-to-mat [matrix mat array-fn]
(map-indexed
 (fn[i line]
  (map-indexed
    (fn[j val] (.put mat i j (array-fn [val])))
    line))
matrix))

(defn matrix-to-matofpoint2f[pts]
(cv/new-matofpoint2f
(into-array org.opencv.core.Point
  (map #(cv/new-point (first %) (second %)) pts))))

(defn matrix-to-mat [matrix]
  (let[
    flat (flatten matrix)
    rows (count matrix)
    cols (count (first matrix))
    mat (Mat. rows cols CvType/CV_32F)
    total (.total mat)
    bytes (float-array total)
    ]
    (doseq [^int i (range 0 total)]
      (aset-float bytes i (nth flat i)))
    (.put mat 0 0 bytes)
    mat))


(defn matrix-to-mat-of-double [matrix]
  (let[
    flat (flatten matrix)
    rows (count matrix)
    cols (count (first matrix))
    mat (cv/new-mat rows cols cv/CV_64F)
    total (.total mat)
    bytes (double-array total)
    ]
    (doseq [^int i (range 0 total)]
      (aset-double bytes i (nth flat i)))
    (.put mat 0 0 bytes)
    mat))

(defn scalar->bytes [s]
  (byte-array (take 3 (.val s ))))

(defn scalar-matrix-to-mat [matrix]
  (let [ row (count matrix) col (count (first matrix)) b (cv/new-mat row col cv/CV_8UC3)]
   (for [i row j col]
     (.put b i j (scalar->bytes (nth (nth matrix i) j ))))
   b))

(defn mat-from [src]
  (Mat. (.rows src) (.cols src) (.type src)))

(defn mat<-url [url option]
  (let [
        tmp-file (File/createTempFile "temp-image" ".tmp")
        fos (FileOutputStream. tmp-file)
        url (URL. url)
        connection (.openConnection url)
       ]
  (.setRequestProperty connection
      "User-Agent"
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31")
(.transferFrom  
  (.getChannel fos) 
  (Channels/newChannel (.getInputStream connection)) 
  0 Long/MAX_VALUE)
(.close fos)
(.disconnect connection)
  (cv/imread (.getAbsolutePath tmp-file) option)))

(defn mat-from-url
  ([url]
  (image->mat (image<-url url)))
  ([url option]
  (mat<-url url option)))

;;;
; MAT OPERATIONS
;;;
(defn resize-by[ mat factor]
  (let [height (.rows mat) width (.cols mat)]
    (cv/resize! mat (cv/new-size (* width factor) (* height factor)) )))

(defn split3! [org]
  (let [splits (cv/split! org)
        [r g b :as arrays] (reverse (map cv/->bytes splits))]
    (byte-array (for [ar arrays i ar] i)))) 
(def mat->flat-rgb-array split3!)

(defn merge3! [mat bytes]
  (let [h (.height mat)
        w (.width mat)
        spatial-size (* h w)
        byte-arrays (reverse (partition spatial-size bytes))
        mats (map #(->cv1 % h w) byte-arrays)]
    (cv/merge! mats mat)))

(defn pixel-map!
  "Applies a function to each pixel. Very slow."
  [im fn_]
  (println "Try to avoid using pixel-map ...")
  (let [buffer (cv/->bytes im)
        pixels (partition 3 buffer)]
    (->> pixels
         (map fn_ )
         (flatten)
         (byte-array)
         (.put im 0 0))
    im))

;;;;
; GEOMETRY
;;;;

(defn middle-of-two-points [p1 p2]
  (cv/new-point
    (/ (+ (.-x p1) (.-x p2)) 2)
    (/ (+ (.-y p1) (.-y p2)) 2)))

(defn center-of-rect [ rect ]
  (middle-of-two-points (.tl rect) (.br rect)))

(defn distance-of-two-points [p1 p2]
  (let [ xd (- (.x p1) (.x p2)) yd (- (.y p1) (.y p2))]
  (Math/sqrt (+ (Math/pow xd 2) (Math/pow yd 2)))))


(defn annotate! [mat text]
 (cv/put-text mat
  text (cv/new-point 100 100) cv/FONT_HERSHEY_PLAIN 1 (cv/new-scalar 255 0 0) 1)
 mat)

;;;
; DRAW CONTOURS
;;;
(defn draw-contours-with-rect!
([buffer contours] (draw-contours-with-rect! buffer contours false))
([buffer contours save-frame]
  (doseq [c contours]
  (let [area (cv/contour-area c)]
     (if (> area 1000)
       (let [rect (cv/bounding-rect c)]
       (if save-frame (cv/imwrite (.submat buffer rect) (str "video/" (java.util.Date.) ".png")))
       (cv/rectangle
         buffer
         (cv/new-point (.x rect) (.y rect))
         (cv/new-point (+ (.width rect) (.x rect)) (+ (.y rect) (.height rect)))
         (cv/new-scalar 255 0 0) 3))))
         buffer)))

(defn draw-contours-with-line! [img contours]
(dotimes [i (.size contours)]
   (let [c (.get contours i)
      m2f (cv/new-matofpoint2f (.toArray c))
      len (cv/arc-length m2f true)
      ret (cv/new-matofpoint2f)
      approx (cv/approx-poly-dp m2f ret (* 0.005 len) true)
      nb-sides (.size (.toList ret))]
(cv/draw-contours img contours i
 (condp = nb-sides
  3  (cv/new-scalar 56.09 68.05 66.27)
  4  (cv/new-scalar 356.09 51.57 43.73)
  7  (cv/new-scalar 26.09 51.57 43.73)
  8  (cv/new-scalar 146.09 51.57 43.73)
  9  (cv/new-scalar 266.09 51.57 43.73)
  10 (cv/new-scalar 236.09 51.57 43.73)
  11 (cv/new-scalar 206.09 68.05 66.27)
  12 (cv/new-scalar 0 0 255)
     (cv/new-scalar 127 50 0))
  -1)))
 img)

;;;
; SWING
;;;

(defn re-show [pane mat]
  (let[image (.getIcon (first (.getComponents pane)))]
  (.setImage image (mat-to-buffered-image mat))
  (doto pane
    (.revalidate)
    (.repaint))))
(defn show
  ([src] (show src {}))
  ([src _options]
  (let [
    options (merge-with merge {:frame {:color "00" :title "image" :width 400 :height 400}} _options)
    is-atom? (= (class src) clojure.lang.Atom)
    buf (if is-atom? (mat-to-buffered-image @src) (mat-to-buffered-image src))
    frame (JFrame. (-> options :frame :title))
    pane (.getContentPane frame)
    image (ImageIcon. buf)
    label (JLabel. image)
    history (cv/new-arraylist)
    get-src (fn[] (buffered-image-to-mat (.getImage  (.getIcon (first (.getComponents pane))))))
    ]
    (println options)
    (doto pane
     (.setOpaque true)
     (.setPreferredSize (java.awt.Dimension. (-> options :frame :width)  (-> options :frame :height) ))
     (.setBackground (java.awt.Color/decode (-> options :frame :color)))
     (.setLayout (FlowLayout.))
     (.add label))
    ; (.addComponentListener frame
    ;   (proxy [java.awt.event.ComponentListener] []
    ;     (componentMoved [event])
    ;     (componentResized [event]
    ;       (re-show pane (cv/resize! (get-src) (cv/new-size (.getWidth frame) (.getHeight frame)))))))
    (.addKeyListener frame
      (proxy [KeyListener] []
        (keyTyped [event])
        (keyReleased [event])
        (keyPressed [event]
          (let [c (.getKeyCode event) handler (-> options :handlers (get c))]
          ;(println c ">" handler)
          (if (not (nil? handler))
             (re-show pane (handler (get-src))))
           (condp = c
             32 (if (.getClientProperty pane "paused")
                  (.putClientProperty pane "paused" false)
                  (.putClientProperty pane "paused" true))
             83 (ImageIO/write
                 (.getImage (.getIcon label))
                 "png" (clojure.java.io/as-file (str (-> options :frame :title) "_" (System/currentTimeMillis) ".png")))
             70  (let [ dsd   (->
                      (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment)
                      (.getDefaultScreenDevice)) ]
                    (if (.getClientProperty pane "fullscreen")
                    (do
                      (.putClientProperty pane "fullscreen" nil)
                      (.setFullScreenWindow dsd nil))
                    (do
                      (.putClientProperty pane "fullscreen" true)
                      (.setFullScreenWindow dsd frame))))
             81  (do
               (.putClientProperty pane "quit" true)
               (.dispose frame))
             (do)
             )))))
    (.addMouseListener label
      (proxy [MouseAdapter] []
       (mousePressed [event])))
    (if is-atom?
       (add-watch src :cat
         (fn [key ref old new-s]
           (if (empty? history) (.add history old))
           (.add history new-s)
           (re-show pane @src))))
    (doto frame
      (.setPreferredSize (java.awt.Dimension. (-> options :frame :width)  (-> options :frame :height) ))
      (.setVisible true)
      (.pack)
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE))
      pane)))
(def imshow show)


(defn simple-cam-window
  ([myvideofn] (simple-cam-window {} myvideofn ))
  ([_options myvideofn]

  (let [
    options (merge-with merge {:frame {:color "00" :title "video"} :video {:device 0 :width 200 :height 220}} _options )
    capture (vid/new-videocapture)
    window (show (cv/new-mat (-> options :video :width) (-> options :video :height)   cv/CV_8UC3 (cv/new-scalar 255 255 255)) options)
    buffer (cv/new-mat)

    ]

    (doto capture
      (.open (-> options :video :device))
      (.set vid/CAP_PROP_FRAME_WIDTH (-> options :video :width))
      (.set vid/CAP_PROP_FRAME_HEIGHT (-> options :video :height)))

    (.start (Thread.
      (fn []
      (while (nil? (.getClientProperty window "quit"))
       (if (.read capture buffer)
        (if (not (.getClientProperty window "paused"))
         (re-show window (myvideofn (cv/clone buffer))))))
       (.release capture)))))))

(defn- start-cam-thread [ window device-map buffer-atom]
    (.start (Thread.
      (fn []
        (println ">> Starting: " (-> device-map :device) " << ")
        (let [ buffer (cv/new-mat) capture (vid/new-videocapture)]
        (doto capture
          (.open (str (-> device-map :device)))
          (.set vid/CAP_PROP_FRAME_WIDTH (-> device-map :width))
          (.set vid/CAP_PROP_FRAME_HEIGHT (-> device-map :height)))
      (while (nil? (.getClientProperty window "quit"))
       (if (.read capture buffer)
        (if (not (.getClientProperty window "paused"))
         (reset! buffer-atom ((-> device-map :fn) (cv/clone buffer))))))
       (.release capture))))))

(defn cams-window[ _options ]
 (let [
   options        (merge-with merge {:frame {:color "00" :title "video"}} _options )
   showing         (nil? (-> options :frame :hide))
   devices        (-> options :devices)
   devices-count  (count devices)
   buffer-atoms   (into [] (map (fn [_] (atom (cv/new-mat))) devices))
   window         (show (cv/new-mat 100 100 cv/CV_8UC3 (cv/new-scalar 0 0 0)) options)
   output         (cv/new-mat)
   outputVideo     (vid/new-videowriter)
   recording       (-> options :video :recording)
   ]
   (doall
     (map #(start-cam-thread window %2 (get buffer-atoms %1)) (range) devices))

     (if (not (nil? recording))
       (do
         (println "Recording Mode")
         (.open
         outputVideo
         (get-in recording [:output] "opencv4.avi")
         1196444237
         (get-in recording [:frame-rate] 30)
         (cv/new-size (get-in recording [:width] 400) (get-in recording [:height] 300) ))))

   (.start (Thread.
    (fn []
    (while (nil? (.getClientProperty window "quit"))
      (if (not (.getClientProperty window "paused"))
            (do
            (if (= (count (filter #(= % 0)  (map #(.cols (deref %)) buffer-atoms))) 0)
            (let [ output (apply (-> options :video :fn) (into [] (map deref buffer-atoms))) ]
             (if showing (re-show window output))
             (if (not (nil? recording))
              (.write outputVideo
               (->
                 output
                 (cv/clone)
                 (cv/resize!
                   (cv/new-size (get-in recording [:width] 400) (get-in recording [:height] 300)))))))))))

            (.release outputVideo))))))
