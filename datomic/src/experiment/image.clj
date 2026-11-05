(ns experiment.image
  (:require [clojure.data.json :as json]
            [cognitect.caster]
            [datomic.core.math]
            [datomic.core.db])
  (:import [cognitect.caster Caster]
           [java.util Random]
           [java.util.concurrent BlockingQueue]))

(def caster-state (atom nil))

(defn prepare! []
  ;; Load AOT namespaces with Clojure's thread bindings before parallel analysis.
  (doseq [{:strs [type]} (get (json/read-str (slurp "target/native-config/reachability-metadata.json")) "reflection")
          :when (and (string? type) (.endsWith ^String type "__init"))]
    (load (str "/" (.replace ^String (subs type 0 (- (count type) 6)) \. \/))))
  (let [^Caster caster @#'cognitect.caster/instance]
    (reset! caster-state {:queue (.-q caster)
                         :callbacks (into {} (map (fn [[k v]] [k @v]) (.-callbacks caster)))})
    (.close caster)
    (.clear ^BlockingQueue (.-q caster))
    (alter-var-root #'cognitect.caster/instance (constantly nil)))
  (doseq [v [#'datomic.core.math/*rnd* #'datomic.core.db/part-rand]]
    (alter-var-root v (constantly nil))))

(defn start! []
  ;; JVM runs already have these objects; only the native image needs new ones.
  (when (nil? @#'cognitect.caster/instance)
    (doseq [v [#'datomic.core.math/*rnd* #'datomic.core.db/part-rand]]
      (alter-var-root v (constantly (Random.))))
    (let [^Caster caster (#'cognitect.caster/create (:queue @caster-state))]
      (doseq [[k handlers] (:callbacks @caster-state)]
        (reset! (get (.-callbacks caster) k) handlers))
      (alter-var-root #'cognitect.caster/instance (constantly caster)))))
