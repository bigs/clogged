(ns clogged.core
  (:import (java.net ServerSocket Socket)
           (java.io BufferedReader InputStreamReader PrintWriter
                    BufferedWriter FileWriter)))

(defn make-server [port]
  (ServerSocket. port))

(defn server-accept [server]
  (.accept server))

(def file-writer
  (agent nil))

(defn log [msg]
  "Sends a write to the file agent"
  (let [write (fn [out msg] (.write out msg) (.flush out) out)]
    (send-off file-writer write msg)))

(defn handler [socket]
  "Create a handler function for a socket.  It will loop, blocking on
   new lines, over the input, sending each line to the agent to be written."
  (fn []
    (let [reader (BufferedReader. (InputStreamReader. (.getInputStream socket)))
          writer (PrintWriter. (.getOutputStream socket) true)]
      (loop [line (.readLine reader)]
        (if (nil? line)
          nil
          (do
            (log (str line "\n"))
            (.println writer "OK")
            (recur (.readLine reader))))))))

(defn server-loop [server]
  "A loop that accepts connections and spawns threads to handle them"
  (loop [client (server-accept server)]
    (.start (Thread. (handler client)))
    (recur (server-accept server))))

(defn usage []
  "Usage"
  (println "USAGE: lein [trampoline] run <filename> <port>"))

(defn -main
  "Start the log server loop"
  [& args]
  (let [filename (first args)
        port (second args)
        server (when port (make-server (Integer/parseInt port)))]
    (if (and filename port)
      (do
        (send file-writer (fn [_] (BufferedWriter. (FileWriter. filename))))
        (server-loop server))
      (usage))))
