(ns braid.server.identicons
  (:require [braid.server.digest :refer [sha256-digest]])
  (:import (java.awt.image BufferedImage
                           AffineTransformOp)
           java.awt.geom.AffineTransform
           java.io.ByteArrayOutputStream
           org.apache.commons.codec.binary.Base64OutputStream
           java.awt.Color
           javax.imageio.ImageIO))

;; General creating icons
(defn new-icon
  "Create a new BufferedImage to draw an icon"
  [w h]
  (BufferedImage. w h BufferedImage/TYPE_INT_RGB))

(defn icon-context
  "Get the graphics context from an icon"
  [icon]
  (.createGraphics icon))

(defmulti draw-identicon (fn [icn-type nums ctx] icn-type))

;; Default-style identicon

(defmethod draw-identicon :default
  [_ [w h] nums]
  (let [img (new-icon w h)
        ctx (icon-context img)
        tiles-per-side 5
        tile-size (quot (min w h) tiles-per-side)
        tiles-count (/ (* tiles-per-side tiles-per-side) 2) ; / 2 b/c we mirror pixels
        mirror-vert? (even? (last nums))
        in-row (fn [pos] (quot pos (/ tiles-per-side 2)))
        in-col (fn [pos] (rem pos (/ tiles-per-side 2)))
        draw-tile (fn [pos]
                    ; draw the tile and it's mirror, to make a symmetrical image
                    (.fillRect
                      ctx
                      (* tile-size (in-col pos))
                      (* tile-size (in-row pos))
                      tile-size tile-size)
                    (.fillRect
                      ctx
                      (* tile-size (- tiles-per-side (in-col pos) 1))
                      (* tile-size (in-row pos))
                      tile-size tile-size))
        pixel-nums (take tiles-count nums)
        [r g b] (take 3 (reverse nums))]
    ; fill bg with white
    (.setColor ctx (Color/WHITE))
    (.fillRect ctx 0 0 w h)
    ; draw pixels
    (.setColor ctx (Color. r g b))
    (loop [nums pixel-nums]
      (when (seq nums)
        (let [pos (- tiles-count (count nums))]
          (when (even? (first nums))
            (draw-tile pos )))
        (recur (rest nums))))
    ; rotate 90 if we want it mirrored horizontally instead of vertically
    (if mirror-vert?
      (let [tx (doto (AffineTransform.)
                 (.rotate (/ Math/PI 2) (/ w 2) (/ h 2)))
            op (AffineTransformOp. tx AffineTransformOp/TYPE_BILINEAR)]
        (.filter op img nil))
      img)))

;; Entry points
(defn uuid->bytes
  [id]
  (->> id str .getBytes sha256-digest seq (map (partial + 128))))

(defn id->identicon-data-url
  ([id] (id->identicon-data-url id :default))
  ([id icon-type] (id->identicon-data-url id :default [120 120]))
  ([id icon-type [w h :as size]]
   (let [img (draw-identicon icon-type size (uuid->bytes id))
         os (ByteArrayOutputStream.)
         b64 (Base64OutputStream. os true 0 nil)]
     (ImageIO/write img "png" b64)
     (str "data:image/png;base64," (.toString os "UTF-8")))))
