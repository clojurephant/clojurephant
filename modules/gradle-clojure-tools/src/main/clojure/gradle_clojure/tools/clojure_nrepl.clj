(ns gradle-clojure.tools.clojure-nrepl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [clojure.core.server :as server]))

(def stopper (atom nil))

(defn start! [repl-port control-port]
  (reset! stopper (promise))
  (server/start-server {:name "control"
                        :port control-port
                        :accept 'gradle-clojure.tools.clojure-nrepl/stop!})
  (let [server (nrepl/start-server :port repl-port)]
    (println "nREPL server started on port " repl-port)
    (println "Enter Ctrl-D to stop the REPL.")
    (deref (deref stopper))
    (server/stop-server "control")
    (nrepl/stop-server server)))

(defn stop! []
  ; message doesn't matter, we're just letting them know it's done
  (deliver @stopper "stop"))
