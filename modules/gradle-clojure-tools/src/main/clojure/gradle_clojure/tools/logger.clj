(ns gradle-clojure.tools.logger
  "Simple logging namespace for tools code that won't conflict with any other logging libraries. Gradle treats all
   output from workers as System.out anyway, so a different logging framework wouldn't provide much more."
  (:require [clojure.string :as string]))

(def ^:private levels {:debug 1 :info 2 :lifecycle 3 :warn 4 :quiet 5 :error 6})

(def ^:private enabled-level (->> "gradle-clojure.tools.logger.level"
                                  (System/getProperty)
                                  (keyword)
                                  (get levels)))

(defmacro log
  "If the given level is enabled, the message will be logged out as follows:
   (log :warn \"Winning at %s\" \"checkers\")
   WARN Winning at checkers"
  [level msg & args]
  (if (<= (or enabled-level 0) (get levels level))
    `(println (string/upper-case (name ~level)) (format ~msg ~@args))))
