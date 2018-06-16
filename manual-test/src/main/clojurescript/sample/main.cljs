(ns sample.main)

(defn work! []
  (let [header (.createElement js/document "h1")
        body (.-body js/document)]
    (set! (.-innerText header) "Now!")
    (.appendChild body header)))
