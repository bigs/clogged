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
  (let [write (fn [out msg] (.write out msg) out)]
    (send-off file-writer write msg)))

(defn flush-to-file [interval]
  "Flushes buffer to file."
  (fn []
    (let [flush-fn (fn [out] (.flush out) out)]
      (loop []
        (Thread/sleep interval)
        (send file-writer flush-fn)
        (recur)))))

(defn handler [socket secret]
  "Create a handler function for a socket.  It will loop, blocking on
   new lines, over the input, sending each line to the agent to be written."
  (fn []
    (let [reader (BufferedReader.
                   (InputStreamReader.
                     (.getInputStream socket)))
          writer (PrintWriter. (.getOutputStream socket) true)]
      (loop [line (.readLine reader)]
        (let [secret-match (when line (re-find #"^(.+?)\W(.+)$" line))
              client-secret (when secret-match (second secret-match))
              log-string (when secret-match (apply str
                                              (interpose " "
                                                (rest (rest secret-match)))))]
          (if (nil? line)
            nil
            (if (not (= secret client-secret))
              (do
                (.println writer "BAD")
                (.close writer)
                (.close reader)
                (.close socket))
              (do
                (log (str log-string "\n"))
                (.println writer "OK")
                (recur (.readLine reader))))))))))

(defn server-loop [server secret]
  "A loop that accepts connections and spawns threads to handle them"
  (loop [client (server-accept server)]
    (.start (Thread. (handler client secret)))
    (recur (server-accept server))))

(defn usage []
  "Usage"
  (println "USAGE: lein [trampoline] run <config file>"))

(defn safe-load [filename]
  (try
    (load-file filename)
    (catch Exception e
      (println "Invalid file"))
    (finally nil)))

(defn -main
  "Start the log server loop"
  [& args]
  (let [config (safe-load (first args))
        port (when config (config :port))
        filename (when config (config :filename))
        secret (when config (config :secret))
        interval (when config (config :interval))
        server (when config (make-server port))]
    (if (and filename port secret)
      (do
        (send file-writer (fn [_] (BufferedWriter. (FileWriter. filename))))
        (.start (Thread. (flush-to-file interval)))
        (server-loop server secret))
      (usage))))
