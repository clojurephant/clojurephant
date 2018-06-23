(ns sample.main
  (:require-macros [sample.main :refer [add]]))

(defn work! []
  (let [header (.createElement js/document "h1")
        body (.-body js/document)]
    (set! (.-innerText header) (str "Now! 1 + 2 is " (add 1 2)))
    (.appendChild body header)))
