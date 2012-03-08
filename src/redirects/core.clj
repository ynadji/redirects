(ns redirects.core
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as html])
  (:use [clojure.string :only (split)])
  (:import [java.net URL]))

(def rfc-uri-regex #"^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?")

(defn uri-query-parse
  [q]
  (into {} (map #(split % #"=")
                (split q #"&"))))

(defn uri-parse
  [uri]
  (let [[_ _ proto _ fqdn path _ query _ anchor]
        (re-matches rfc-uri-regex uri)]
    {:proto proto
     :fqdn fqdn
     :path path
     :query (when query (uri-query-parse query))
     :anchor anchor}))

(defn foo [x]
  (map #(* % %) (range x)))

(def tmp (client/get "http://www.google.com"))

(defn redirects
  [url]
  ["domains" "urls"])

(defn parse-page
  [s]
  (let [urls (distinct (map #(:href (:attrs %))
                            (-> s java.io.StringReader. html/html-resource
                                (html/select [:a]))))]
    urls))

(defn http-redirects
  [url]
  (:trace-redirects (client/get url)))

(defn unredirect
  [url]
  (http-redirects url))