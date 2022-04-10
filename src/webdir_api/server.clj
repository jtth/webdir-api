(ns webdir-api.server
  (:require [webdir-api.service :as service]
            [io.pedestal.http :as bootstrap])
  (:gen-class))


(defonce service-instance nil)

(defn create-server []
  (alter-var-root #'service-instance
                  (constantly (bootstrap/create-server
                                (-> service/service
                                    (assoc ::bootstrap/port (Integer. (or (System/getenv "PORT") 8080)))
                                    (bootstrap/default-interceptors))))))

(defn start []
  (when-not service-instance
    (create-server))
  (println "Starting server on port" (::bootstrap/port service-instance))
  (bootstrap/start service-instance))

(defn stop []
  (when service-instance
    (bootstrap/stop service-instance)))

(defn restart []
  (stop)
  (start))

(defn -main [& args]
  (start))

;; Rich comment for REPL control
(comment
  (start)
  (stop)
  (restart))