(ns redirects.core
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as html])
  (:use [clojure.string :only (split)])
  (:import [java.net URL]))

;;;; Utilities
(defn- resourcify [page] (-> page java.io.StringReader. html/html-resource))

(def rfc-uri-regex #"^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?")

(defn uri-query-parse
  [q]
  (into {} (map #(split % #"=")
                (split q #"&"))))

(defn uri-parse
  [uri]
  (let [[_ _ proto _ fqdn path _ query _ anchor]
        (re-matches rfc-uri-regex (.toLowerCase uri))]
    {:proto proto
     :fqdn fqdn
     :path path
     :query (when query (uri-query-parse query))
     :anchor anchor}))

(defn- valid-url?
  [uri]
  (let [parsed (uri-parse uri)]
    (if (and (or (= "http" (:proto parsed))
                 (= "https" (:proto parsed)))
             (not (nil? (:fqdn parsed))))
      uri
      nil)))

(def tmp (client/get "http://www.google.com"))
(def tmpres (resourcify (:body tmp)))
(def iit (client/get "http://mypages.iit.edu/~ynadji/"))
(def iitres (resourcify (:body iit)))

(defn redirects
  [url]
  ["domains" "urls"])

(defn parse-body
  "Given HTML source as a string, parse and return all URLs in the source."
  [body]
  (let [urls (distinct (map #(:href (:attrs %))
                            (-> body resourcify
                                (html/select [:a]))))]
    urls))

(defn- parse-meta-redirect
  "Given HTML source as string, parse and return the URL of a <meta> redirect
  or nil if it doesn't exist."
  [body]
  (try (valid-url?
        (second (split (-> body resourcify
                           (html/select [:head :meta]) first :attrs :content) #"=")))
       (catch java.lang.NullPointerException e nil)))

(defn http-redirects
  "Handle all HTTP redirects."
  [url]
  (let [http-reply (client/get url)
        standard-redirects (:trace-redirects http-reply)
        meta-redirect (if (= 1 (count standard-redirects))
                        (-> http-reply :body parse-meta-redirect)
                        (-> (last standard-redirects) client/get :body parse-meta-redirect))]
    (if meta-redirect
      (conj standard-redirects meta-redirect)
      standard-redirects)))

(defn unredirect
  [url]
  (http-redirects url))