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

(defn- parse-meta-content
  "Parse <meta> tag content attribute. Special cases galore!"
  [content]
  (let [tmp (second (split content #"="))]
    (cond
     (and (.startsWith tmp "'") (.endsWith tmp "'"))
     (subs tmp 1 (dec (count tmp)))
     :else tmp)))

(defn- parse-meta-redirect
  "Given HTML source as string, parse and return the URL of a <meta> redirect
  or nil if it doesn't exist."
  [body]
  (try (valid-url?
        (parse-meta-content (-> body resourcify
                                (html/select [:meta])
                                first :attrs :content)))
       (catch java.lang.NullPointerException e nil)))

(defn- parse-iframe-src
  "Given HTML source as a string, parse and return the URLs in <iframe>'s
  if any exist."
  [body]
  (filter #(not (nil? %))
          (distinct (map #(:src (:attrs %))
                         (-> body resourcify
                             (html/select [:iframe]))))))

(defn http-redirects
  "Handle all HTTP redirects."
  [url & {:keys [standard? meta? iframe? all?] :or
          {standard? true meta? true iframe? true all? false}}]
  (let [http-reply (client/get url)
        standard-redirects (:trace-redirects http-reply)]
    {:standard-redirects (when standard? standard-redirects)
     :meta-redirect (when meta? (if (= 1 (count standard-redirects))
                                 (-> http-reply :body parse-meta-redirect)
                                 (-> (last standard-redirects) client/get :body parse-meta-redirect)))
     :iframes (when iframe? (-> http-reply :body parse-iframe-src))
     :all-hrefs (when all? (-> http-reply :body parse-body))}))

(defn unredirect
  [url]
  (http-redirects url))