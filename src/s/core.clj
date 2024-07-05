(ns s.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

; Constantes utilizadas
(def ^:const AVANCE 100)
(def ^:const ANGULO-INICIAL 270)
(def ^:const CAMBIO-SENTIDO 180)
(def ^:const ARGUMENTOS-REQUERIDOS 3)
(def ^:const ARG-ARCHIVO 0)
(def ^:const ARG-ITERACIONES 1)
(def ^:const ARG-ARCHIVO-SVG 2)
(def ^:const LINEAS 2)
(def ^:const POS-INICIAL 0)
(def ^:const MARGEN-INICIAL 0)

; Estructura de la tortuga.
(defrecord Tortuga [x y angulo])

; Esta función parsea las líneas del archivo, extrae el ángulo (como double), el estado inicial y las reglas.
(defn parsear-lineas [lineas]
  (let [angulo (Double/parseDouble (first lineas))
        estado-inicial (second lineas)
        reglas (apply hash-map (mapcat #(str/split % #" ") (drop LINEAS lineas)))]
    [angulo estado-inicial reglas]))

; FUNCION IMPURA! Esta función abre el archivo, lo lee e invoca a la función de parseo.
; Es impura porque tiene contacto con el exterior (el archivo).
(defn leer-archivo [ruta-archivo]
  (with-open [rdr (io/reader ruta-archivo)]
    (let [lineas (line-seq rdr)]
      (parsear-lineas lineas))))

; Esta función aplica las reglas de conversión al estado actual.
(defn aplicar-reglas [estado reglas]
  (apply str (map (fn [c] (get reglas (str c) (str c))) estado)))

; Esta función realiza las iteraciones del sistema-L.
(defn iteraciones-sistema-l [estado-inicial reglas iteraciones]
  (loop [estado-actual estado-inicial
         n iteraciones]
    (if (zero? n)
      estado-actual
      (recur (aplicar-reglas estado-actual reglas) (dec n)))))

; Crea la tortuga inicial.
(defn crear-tortuga-inicial []
  (->Tortuga POS-INICIAL POS-INICIAL ANGULO-INICIAL))

; Crea una tortuga nueva a partir de la original.
(defn crear-tortuga [tortuga]
  (let [{:keys [x y angulo]} tortuga]
    (->Tortuga x y angulo)))

; Hace el avance de la tortuga en relación al ángulo actual de la misma.
(defn avanzar [tortuga n]
  (let [rads (Math/toRadians (:angulo tortuga))
        dx (* n (Math/cos rads))
        dy (* n (Math/sin rads))
        new-x (+ (:x tortuga) dx)
        new-y (+ (:y tortuga) dy)]
    (assoc tortuga :x new-x :y new-y)))

; Cambia el ángulo de la tortuga hacia la derecha.
(defn gira-derecha [tortuga angulo]
  (assoc tortuga :angulo (+ (:angulo tortuga) angulo)))

; Cambia el ángulo de la tortuga hacia la izquierda.
(defn gira-izquierda [tortuga angulo]
  (assoc tortuga :angulo (- (:angulo tortuga) angulo)))

; Actualiza el camino actual que "pinto" la tortuga.
(defn actualizar-camino [tortuga nueva-tortuga camino]
  (conj camino {:x1 (:x tortuga) :y1 (:y tortuga) :x2 (:x nueva-tortuga) :y2 (:y nueva-tortuga)}))

; Ejecuta el comando recibido por parámetro (carácter del sistema-L).
(defn ejecutar-comando [comando angulo tortugas camino]
  (let [tortuga (first tortugas)]
    (cond
      (#{\F \G} comando)
      (let [nueva-tortuga (avanzar tortuga AVANCE)]
        [(conj (rest tortugas) nueva-tortuga) (actualizar-camino tortuga nueva-tortuga camino)])
      (#{\f \g} comando) [(conj (rest tortugas) (avanzar tortuga AVANCE)) camino]
      (= comando \+) [(conj (rest tortugas) (gira-derecha tortuga angulo)) camino]
      (= comando \-) [(conj (rest tortugas) (gira-izquierda tortuga angulo)) camino]
      (= comando \[) [(conj tortugas (crear-tortuga tortuga)) camino]
      (= comando \]) [(pop tortugas) camino]
      (= comando \|) [(conj (rest tortugas) (gira-derecha tortuga CAMBIO-SENTIDO)) camino]
      :else [tortugas camino])))

; Función para procesar los comandos generados por el sistema-L y obtener el camino final.
(defn procesar-comandos [estado-final angulo]
  (loop [comandos estado-final
         tortugas [(crear-tortuga-inicial)]
         camino []]
    (if (empty? comandos)
      camino
      (let [comando (first comandos)
            [nuevas-tortugas nuevo-camino] (ejecutar-comando comando angulo tortugas camino)]
        (recur (rest comandos) nuevas-tortugas nuevo-camino)))))

; Calcula la máxima y la mínima posición en "X" e "Y" del camino creado.
(defn calcular-limites [camino margen]
  (let [x-values (mapcat #(vector (:x1 %) (:x2 %)) camino)
        y-values (mapcat #(vector (:y1 %) (:y2 %)) camino)
        min-x (- (apply min x-values) margen)
        max-x (+ (apply max x-values) margen)
        min-y (- (apply min y-values) margen)
        max-y (+ (apply max y-values) margen)]
    [min-x max-x min-y max-y]))

; Traduce el texto (en base al camino y al margen) que va en el archivo "SVG" para crear la imagen.
(defn generar-svg [camino margen]
  (let [[min-x max-x min-y max-y] (calcular-limites camino margen)
        view-box (str min-x " " min-y " " (- max-x min-x) " " (- max-y min-y))
        svg-header (str "<svg viewBox=\"" view-box "\" xmlns=\"http://www.w3.org/2000/svg\">")
        svg-content (clojure.string/join "" (map #(str "<line x1=\"" (:x1 %) "\" y1=\"" (:y1 %) "\" x2=\"" (:x2 %) "\" y2=\"" (:y2 %) "\" stroke-width=\"4\" stroke=\"black\" />\n") camino))
        svg-footer "</svg>"]
    (str svg-header svg-content svg-footer)))

; FUNCION IMPURA: Esta función (junto con generar-svg) crea el archivo ".SVG" en base al texto generado para el mismo.
; Es impura porque tiene contacto con el exterior (el SVG).
(defn guardar-svg-en-archivo [camino nombre-archivo]
  (let [contenido-svg (generar-svg camino MARGEN-INICIAL)]
    (spit nombre-archivo contenido-svg)))

(defn -main
  [& args]
  (if (< (count args) ARGUMENTOS-REQUERIDOS)
    (println "Por favor, proporciona el nombre del archivo, el numero de iteraciones y el nombre del archivo SVG a generar como argumentos.")
    (let [nombre-archivo (nth args ARG-ARCHIVO)
          iteraciones (Integer/parseInt (nth args ARG-ITERACIONES))
          nombre-archivo-svg (nth args ARG-ARCHIVO-SVG)
          ruta-archivo (io/file "doc" nombre-archivo)
          ruta-svg (io/file (.getParent ruta-archivo) nombre-archivo-svg)]
      (if (and nombre-archivo iteraciones nombre-archivo-svg)
        (do
          (let [informacion-parseada (leer-archivo ruta-archivo)
                [angulo estado-inicial reglas] informacion-parseada
                estado-final (iteraciones-sistema-l estado-inicial reglas iteraciones)
                camino-final (procesar-comandos estado-final angulo)]
                (guardar-svg-en-archivo camino-final ruta-svg)
                (println "Imagen generada de forma exitosa.")))))))