(ns redirects.core)

(require '[clj-http.client :as client])

(defn foo [x]
  (map #(* % %) (range x)))

(def tmp (client/get "http://www.google.com"))

(defn redirects
  [url]
  ["domains" "urls"])

(defn http-redirects
  [url]
  (:trace-redirects (client/get url)))

