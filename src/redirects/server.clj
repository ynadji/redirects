(ns redirects.server
  (:use [redirects.core :only (unredirect)]
        [server.socket :only (create-server)]
        [clojure.java.io :only (reader writer)])
  (:import [java.net ServerSocket SocketException]
           [java.io PrintWriter BufferedReader InputStreamReader OutputStreamWriter]))

(def exit? (atom false))

(defn- join
  [sep elements]
  (apply str (interpose "\n" elements)))

;(close-server foo)

(defn echo
  [input output]
  (with-open [in (reader input)
              out (writer output)]
    (.write out (join "\n" (unredirect (.readLine in))))
    (.write out "\n")
    (.flush out)))

(defn -main []
  (def foo (create-server 1234 echo)))