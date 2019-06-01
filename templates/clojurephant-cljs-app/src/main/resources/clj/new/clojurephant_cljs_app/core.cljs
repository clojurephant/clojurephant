(ns {{namespace}}
  (:require-macros [{{namespace}} :refer [add]]))

(defn message [a b]
  (str a " plus " b " is " (add a b)))

(defn add-elem [root type message]
  (let [elem (.createElement js/document type)]
    (set! (.-innerText elem) message)
    (.appendChild root elem)))

(def app (.getElementById js/document "app"))

(set! (.-innerHTML app) "")

(add-elem app "h1" "Connected to Figwheel!")
(add-elem app "p" "Any edits to your code will auto-refresh in the browser. Thank you Figwheel!")
(add-elem app "p" (message 2 3))
