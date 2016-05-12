(ns braid.desktop.draggable
  (:require [cljs.core.async :as async :refer [put!]]
            [goog.events :as events]
            [goog.style :as gstyle])
  (:import [goog.events EventType]))

(defn gsize->vec [size]
  [(.-width size) (.-height size)])

(defn node-size [node]
  (gsize->vec (gstyle/getSize node)))

(defn to?
  "Is the owner transitioning to k (either in state or props)"
  [owner next-props next-state k]
  (or (and (not (om/get-render-state owner k))
        (k next-state))
      (and (not (k (om/get-props owner)))
        (k next-props))))

(defn from?
  "Is the owner transitioning from k (either in state or props)"
  [owner next-props next-state k]
  (or (and (om/get-render-state owner k)
        (not (k next-state)))
      (and (k (om/get-props owner))
        (not (k next-props)))))

(defn location
  [e]
  [(.-clientX e) (.-clientY e)])

(defn element-offset
  [el]
  (let [offset (gstyle/getPageOffset el)]
    [(.-x offset) (.-y offset)]))

(defn dragging?
  [owner]
  (om/get-state owner :dragging))

(defn drag-start
  [e item owner]
  (when-not (dragging? owner)
    (.preventDefault e)
    (let [el (om/get-node owner "draggable")
          state (om/get-state owner)
          drag-start (location e)
          el-offset (element-offset el)
          drag-offset (vec (map - el-offset drag-start))]
      (when-not (:delegate state)
        (om/set-state! owner :dragging true))
      (doto owner
        (om/set-state! :location ((or (:constrain state) identity) el-offset))
        (om/set-state! :drag-offset drag-offset))
      (when-let [c (:chan state)]
        (put! c {:event :drag-start :id (:id item)
                 :location (vec (map + drag-start drag-offset))})))))

(defn drag-stop
  [e item owner]
  (when (dragging? owner)
    (let [state (om/get-state owner)]
      (when (:dragging state)
        (om/set-state! owner :dragging false))
      (when-not (:delegate state)
        (doto owner
          (om/set-state! :location nil)
          (om/set-state! :drag-offset nil)))
      (when-let [c (:chan state)]
        (put! c {:event :drag-stop :id (:id item)})))))

(defn drag
  [e item owner]
  (let [state (om/get-state owner)]
    (when (dragging? owner)
      (let [loc ((or (:constrain state) identity)
                 (vec (map + (location e) (:drag-offset state))))]
        (om/set-state! owner :location loc)
        (when-let [c (:chan state)]
          (put! c {:event :drag :location loc :id (:id item)}))))))

(defn draggable [item owner opts]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [dims (node-size (om/get-node owner "draggable"))]
        (om/set-state! owner :dimensions dims)
        (when-let [dims-chan (:dims-chan (om/get-state owner))]
          (put! dims-chan dims))))
    om/IWillUpdate
    (will-update [_ next-props next-state]
      (when (to? owner next-props next-state :dragging)
        (let [mouse-up #(drag-stop % @next-props owner)
              mouse-move #(drag % @next-props owner)]
          (om/set-state! owner :window-listeners [mouse-up mouse-move])
          (doto js/window
            (events/listen EventType.MOUSEUP mouse-up)
            (events/listen EventType.MOUSEMOVE mouse-move))))
      (when (from? owner next-props next-state :dragging)
        (let [[mouse-up mouse-move] (om/get-state owner :window-listeners)]
          (doto js/window
            (events/unlisten EventType.MOUSEUP mouse-up)
            (events/unlisten EventType.MOUSEMOVE mouse-move)))))
    om/IRenderState
    (render-state [_ state]
      (let [style (if (dragging? owner)
                    (let [[x y] (:location state)
                          [w h] (:dimensions state)]
                      #js {:position "absolute"
                           :top y :left x :z-index 1
                           :width w :height h})
                    #js {:position "static" :z-index 0})]
        (dom/li #js {:classname (when (dragging? owner) "dragging")
                     :style style
                     :ref "draggable"
                     :onMouseDown #(drag-start % @item owner)
                     :onMouseUp #(drag-stop % @item owner)
                     :onMouseMove #(drag % @item owner)}
          (om/build (:view state) item opts))))))
