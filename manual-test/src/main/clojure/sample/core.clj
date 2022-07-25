(ns sample.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [com.stuartsierra.component :as component]))

(defn hello [name]
  (println "Hello" name))

(defn ends? [string suffix]
  (.endsWith string suffix))

(defn respond-hello [request]
  {:status 200 :body "Hello, Earth!"})

(def routes
  (route/expand-routes
   #{["/greet" :get respond-hello :route-name :greet]}))

(defrecord Pedestal
    [service-map service]
  component/Lifecycle
  (start [component]
    (if service
      component
      (assoc component :service (http/start (http/create-server service-map)))))
  (stop [component]
    (when service
      (http/stop service))
    (assoc component :service nil)))

(defn make-pedestal []
  (map->Pedestal {}))

(defn make-system [_]
  (component/system-map
   :service-map
   {::http/type :jetty
    ::http/port 8080
    ::http/join? false
    ::http/routes routes
    ::http/resource-path "public"
    ::http/secure-headers {:content-security-policy-settings {:object-src "none"}}}
   :pedestal
   (component/using
    (make-pedestal)
    [:service-map])))
