(ns hotload.base
  (:import [clojure.lang DynamicClassLoader RT])
  (:require [cemerick.pomegranate :as pg]))

(defn base-classloader []
  (let [^DynamicClassLoader cl (RT/baseLoader)]
    (if-let [^DynamicClassLoader parent (.getParent cl)]
      parent
      (or cl (.. Thread currentThread getContextClassLoader)))))
      
(defn base-classloader-hierarchy []
  (pg/classloader-hierarchy (base-classloader)))
  
(defn find-top-classloader [classloaders]
  (last (filter pg/modifiable-classloader? classloaders)))