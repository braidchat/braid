(ns chat.client.views.helpers.events
  (:require [om.core :as om]))

(def PIXEL-STEP 10)
(def LINE-HEIGHT 40)
(def PAGE-HEIGHT 800)

(defn signum [x] (if (>= x 0) 1 -1))

(defn normalize-scroll
  [event]
  (let [sY (condp (fn [field obj] (aget obj field)) event
             "detail" :>> identity
             "wheelDelta" :>> #(/ (- %) 120)
             "wheelDeltaY" :>> #(/ (- %) 120)
             0)
        sX (if-let [wdx (.-wheelDeltaX event)]
             (/ (- wdx) 120)
             0)
        [sX sY] (if (and (some? (.-axis event))
                      (= (.-axis event) (.-HORIZONTAL_AXIS event)))
                  [sY 0]
                  [sX sY])
        pX (if-let [dx (.-deltaX event)]
             dx
             (* sX PIXEL-STEP))
        pY (if-let [dy (.-deltaY event)]
             dy
             (* sY PIXEL-STEP))
        [pX pY] (if (and (some? (.-deltaMode event))
                      (= 1 (.-deltaMode event)))
                  [(* pX LINE-HEIGHT) (* pY LINE-HEIGHT)]
                  [(* pX PAGE-HEIGHT) (* pY PAGE-HEIGHT)])
        sX (if (and (not= 0 pX) (zero? sX))
             (signum pX)
             sX)
        sY (if (and (not= 0 pY) (zero? sY))
             (signum pY)
             sY)]
    {:spinX sX
     :spinY sY
     :pixelX pX
     :pixelY pY}))

(defn scroll-threads-handler
  [owner]
  (fn [e]
    (let [target-classes (.. e -target -classList)
          this-elt (om/get-node owner "threads-div")
          scroll (normalize-scroll e)]
      (println "scroll" scroll)
      ; TODO: check if threads-div needs to scroll?
      (when (and (or (.contains target-classes "thread")
                     (.contains target-classes "threads"))
              (= 0 (:pixelX scroll)))
        (set! (.-scrollLeft this-elt)
              (- (.-scrollLeft this-elt)
                 (:pixelY (normalize-scroll e))))))))

