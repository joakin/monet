(ns monet.canvas
  (:require [monet.core :as core]))

;;*********************************************
;; Canvas drawing functions
;;*********************************************

(defn get-context [canvas type]
  (. canvas (getContext (name type))))

(defn begin-path [ctx]
  (. ctx (beginPath))
  ctx)

(defn close-path [ctx]
  (. ctx (closePath))
  ctx)

(defn fill [ctx]
  (. ctx (fill))
  ctx)

(defn stroke [ctx]
  (. ctx (stroke))
  ctx)

(defn clear-rect [ctx {:keys [x y w h]}]
  (. ctx (clearRect x y w h))
  ctx)

(defn rect "DEPRECATED. Prefer fill-rect" [ctx {:keys [x y w h]}]
  (begin-path ctx)
  (. ctx (rect x y w h))
  (close-path ctx)
  (fill ctx)
  ctx)

(defn stroke-rect [ctx {:keys [x y w h]}]
  (. ctx (strokeRect x y w h))
  ctx)

(defn fill-rect [ctx {:keys [x y w h]}]
  (. ctx (fillRect x y w h))
  ctx)

(defn circle [ctx {:keys [x y r]}]
  (begin-path ctx)
  (. ctx (arc x y r 0 (* (.-PI js/Math) 2) true))
  (close-path ctx)
  (fill ctx)
  ctx)

(defn text [ctx {:keys [text x y]}]
  (. ctx (fillText text x y))
  ctx)

(defn font-style [ctx font]
  (set! (.-font ctx) font)
  ctx)

(defn fill-style [ctx color]
  (set! (.-fillStyle ctx) (name color))
  ctx)

(defn stroke-style [ctx color]
  (set! (.-strokeStyle ctx) (name color))
  ctx)

(defn stroke-width [ctx w]
  (set! (.-lineWidth ctx) w)
  ctx)

(defn stroke-cap [ctx cap]
  (set! (.-lineCap ctx) (name cap))
  ctx)

(defn move-to [ctx x y]
  (. ctx (moveTo x y))
  ctx)

(defn line-to [ctx x y]
  (. ctx (lineTo x y))
  ctx)

(defn alpha [ctx a]
  (set! (.-globalAlpha ctx) a)
  ctx)

(defn composition-operation [ctx operation]
  (set! (.-globalCompositionOperation ctx) (name operation))
  ctx)

(defn text-align [ctx alignment]
  (set! (.-textAlign ctx) (name alignment))
  ctx)

(defn text-baseline [ctx alignment]
  (set! (.-textBaseline ctx) (name alignment))
  ctx)

(defn get-pixel
  "Gets the pixel value as a hash map of RGBA values"
  [ctx x y]
  (let [imgd (.-data (.getImageData ctx x y 1 1))]
      { :red   (aget imgd 0)
        :green (aget imgd 1)
        :blue  (aget imgd 2)
        :alpha (aget imgd 3)}))

(defn save [ctx]
  (. ctx (save))
  ctx)

(defn restore [ctx]
  (. ctx (restore))
  ctx)

(defn rotate [ctx angle]
  (. ctx (rotate angle))
  ctx)

(defn scale [ctx x y]
  (. ctx (scale x y))
  ctx)

(defn translate [ctx x y]
  (. ctx (translate x y))
  ctx)

(defn transform
  "Multiplies a custom transformation matrix to the existing
   HTML5 canvas transformation according to the follow convention:

   [ x']   [ m11 m21 dx ] [ x ]
   [ y'] = [ m12 m22 dy ] [ y ]
   [ 1 ]   [ 0   0   1  ] [ 1 ]"
  ([ctx m11 m12 m21 m22 dx dy]
    (. ctx (transform m11 m12 m21 m22 dx dy))
    ctx)
  ([ctx {:keys [m11 m12 m21 m22 dx dy]}]
    (. ctx (transform m11 m12 m21 m22 dx dy))
    ctx))

(defn draw-image
  "Draws the image onto the canvas at the given position.
   If a map of params is given, the number of entries is used to
   determine the underlying call to make."
  ([ctx img x y]
    (. ctx (drawImage img x y))
    ctx)
  ([ctx img {:keys [x y w h
                    sx sy sw sh dx dy dw dh] :as params}]
    (condp = (count params)
      2 (. ctx (drawImage img x y))
      4 (. ctx (drawImage img x y w h))
      8 (. ctx (drawImage img sx sy sw sh dx dy dw dh)))
      ctx))

(defn quadratic-curve-to
  ([ctx cpx cpy x y]
    (. ctx (quadraticCurveTo cpx cpy x y))
    ctx)
  ([ctx {:keys [cpx cpy x y]}]
    (. ctx (quadraticCurveTo cpx cpy x y))
    ctx))

(defn bezier-curve-to
  ([ctx cp1x cp1y cp2x cp2y x y]
    (. ctx (bezierCurveTo cp1x cp1y cp2x cp2y x y))
    ctx)
  ([ctx {:keys [cp1x cp1y cp2x cp2y x y]}]
    (. ctx (bezierCurveTo cp1x cp1y cp2x cp2y x y))
    ctx))

(defn rounded-rect [ctx {:keys [x y w h r]}]
  "Stroke a rectangle with rounded corners of radius r pixels."
  (-> ctx
      begin-path
      (move-to x (+ y r))
      (line-to x (- (+ y h) r))
      (quadratic-curve-to x (+ y h) (+ x r) (+ y h))
      (line-to (- (+ x w) r) (+ y h))
      (quadratic-curve-to (+ x w) (+ y h) (+ x w) (- (+ y h) r))
      (line-to (+ x w) (+ y r))
      (quadratic-curve-to (+ x w) y (- (+ x w) r) y)
      (line-to (+ x r) y)
      (quadratic-curve-to x y x (+ y r))
      stroke)
  ctx)

;;*********************************************
;; Canvas Entities
;;*********************************************

(defn add-entity [mc k ent]
  (aset (:entities mc) k ent))

(defn remove-entity [mc k]
  (js-delete (:entities mc) k))

(defn get-entity [mc k]
  (:value (aget (:entities mc) k)))

(defn update-entity [mc k func & extra]
  (let [cur (aget (:entities mc) k)
        res (apply func cur extra)]
    (aset (:entities mc) k res)))

(defn clear! [mc]
  (let [ks (js-keys (:entities mc))]
    (doseq [k ks]
      (remove-entity mc k))))

(defn entity [v update draw]
  {:value v
   :draw draw
   :update update})

(defn- attr [e a]
  (.getAttribute e a))

(defn draw-loop [{:keys [canvas updating? ctx active entities] :as mc}]
  (clear-rect ctx {:x 0 :y 0 :w (attr canvas "width") :h (attr canvas "height")})
  (when @active
    (let [ks (js-keys entities)
          cnt (alength ks)]
      (loop [i 0]
        (when (< i cnt)
          (let [k (aget ks i)
                {:keys [draw update value] :as ent} (aget entities k)]
            (when (and update @updating?)
              (let [updated (or (try (update value)
                                  (catch js/Error e
                                    (.log js/console e)
                                    value))
                                value)]
                (when (aget entities k)
                  (aset entities k (assoc ent :value updated)))))
            (when draw
              (try
                (draw ctx (:value (aget entities k)))
                (catch js/Error e
                  (.log js/console e))))
            (recur (inc i))))))
    (core/animation-frame #(draw-loop mc))))

(defn monet-canvas [elem context-type]
  (let [ct (or context-type "2d")
        ctx (get-context elem ct)]
    {:canvas elem
     :ctx ctx
     :entities (js-obj)
     :updating? (atom true)
     :active (atom true)}))

(defn init [canvas & [context-type]]
  (let [mc (monet-canvas canvas context-type)]
    ;;(update-loop mc)
    (draw-loop mc)
    mc))

(defn stop [mc] (reset! (:active mc) false))
(defn stop-updating [mc] (reset! (:updating? mc) false))
(defn start-updating [mc] (reset! (:updating? mc) true))
(defn restart [mc]
  (reset! (:active mc) true)
  ;;(update-loop mc)
  (draw-loop mc))
