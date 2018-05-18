(ns gradle-clojure.tools.clojure-nrepl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [clojure.core.server :as server]
            [clojure.edn :as edn]))

(def stopper (atom nil))

(defn resolve-fn [qname]
  (let [sym (symbol qname)
        ns-sym (symbol (namespace sym))]
    (require ns-sym)
    (if-let [resolved (resolve sym)]
      resolved
      (throw (ex-info "Could not resolve function." {:name qname})))))

(defn make-handler [handler middleware]
  (if handler
    (resolve-fn handler)
    (apply nrepl/default-handler (map resolve-fn middleware))))

(defn start! [repl-port control-port handler]
  (reset! stopper (promise))
  (server/start-server {:name "control"
                        :port control-port
                        :accept 'gradle-clojure.tools.clojure-nrepl/stop!})
  (let [server (nrepl/start-server :port repl-port :handler handler)]
    (println "nREPL server started on port" repl-port)
    (println "Enter Ctrl-D to stop the REPL.")
    (deref (deref stopper))
    (server/stop-server "control")
    (nrepl/stop-server server)
    (shutdown-agents)))

(defn stop! []
  ; message doesn't matter, we're just letting them know it's done
  (deliver @stopper "stop"))

(defn -main [& args]
  (let [[repl-port control-port handler middleware] (edn/read)]
    (start! repl-port control-port (make-handler handler middleware))))
