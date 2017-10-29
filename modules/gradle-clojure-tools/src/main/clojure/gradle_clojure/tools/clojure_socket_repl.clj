(ns gradle-clojure.tools.clojure-socket-repl
  (:require [clojure.core.server :as server]))

(def stopper (atom nil))

(defn start! [port]
  (reset! stopper (promise))
  (server/start-server {:name "repl"
                        :port port
                        :accept 'clojure.core.server/repl})
  (deref (deref stopper))
  (server/stop-server "repl"))

(defn stop! []
  ; message doesn't matter, we're just letting them know it's done
  (deliver @stopper "stop"))
