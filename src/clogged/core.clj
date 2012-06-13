(ns clogged.core
  (:import (java.net ServerSocket Socket)
           (java.io BufferedReader InputStreamReader PrintWriter
                    BufferedWriter FileWriter)))

(defn make-server [port]
  "Initialize a server socket."
  (ServerSocket. port))

(defn server-accept [server]
  (.accept server))

(def file-writer
  (agent nil))

(defn log [msg]
  "Sends a write to the file agent."
  (let [write (fn [out msg] (.write out msg) (.flush out) out)]
    (send-off file-writer write msg)))

(defn handler [socket secret]
  "Create a handler function for a socket.  It will loop, blocking on
   new lines, over the input, sending each line to the agent to be written."
  (fn []
    (let [reader (BufferedReader. (InputStreamReader. (.getInputStream socket)))
          writer (PrintWriter. (.getOutputStream socket) true)]
      (loop [line (.readLine reader)
             secret-match (re-find #"^(.+)\W" line)
             client-secret (when secret-match (second secret-match))]
        (if (or (nil? line) (not (= secret client-secret)))
          nil
          (do
            (log (str line "\n"))
            (.println writer "OK")
            (recur (.readLine reader))))))))

(defn server-loop [server secret]
  "A loop that accepts connections and spawns threads to handle them"
  (loop [client (server-accept server)]
    (.start (Thread. (handler client secret)))
    (recur (server-accept server))))

(defn usage []
  "Usage"
  (println "USAGE: lein [trampoline] run <config file>"))

(defn -main
  "Start the log server loop"
  [& args]
  (let [config (load-file (first args))
        port (when config (config :port))
        filename (when config (config :filename))
        secret (when config (config :secret))
        server (when config (make-server port))]
    (if (and filename port secret)
      (do
        (send file-writer (fn [_] (BufferedWriter. (FileWriter. filename))))
        (server-loop server secret))
      (usage))))
