(ns clogged.core
  (:import (java.net ServerSocket Socket)
           (java.io BufferedReader InputStreamReader PrintWriter
                    BufferedWriter FileWriter)))

(defn make-server [port]
  (ServerSocket. port))

(defn server-accept [server]
  (.accept server))

(def file-writer
  (agent (BufferedWriter. (FileWriter. "test.log"))))

(defn log [msg]
  (let [write (fn [out msg] (.write out msg) (.flush out) out)]
    (send-off file-writer write msg)))

(defn handler [socket]
  (let [reader (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        writer (PrintWriter. (.getOutputStream socket) true)]
    (loop [line (.readLine reader)]
      (if (nil? line)
        (println "--DONE--")
        (do
          (log (str line "\n"))
          (.println writer "OK")
          (recur (.readLine reader)))))))

(defn server-loop [server]
  (loop [client (agent (server-accept server))]
    (send-off client handler)
    (recur (agent (server-accept server)))))

(defn -main
  "Start the log server loop"
  [& args]
  (let [server (make-server (Integer/parseInt (first args)))]
    (server-loop server)))
