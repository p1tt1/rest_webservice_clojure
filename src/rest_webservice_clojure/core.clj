(ns rest-webservice-clojure.core
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [rest-webservice-clojure.calc :refer [addUp digitSum]])
  (:gen-class))

(def PORT_DEFAULT 5000)

(def HTTP_METHODS [:post])
(def ACCEPT_CONTENT_TYPES [nil, "*/*", "application/json"])
(def REQUEST_CONTENT_TYPES ["application/json"])
(def ERR_STATUS_MAP {:ERR_HTTP_METHOD 405})

(defn entryOf? [vec entry] (some #(= entry %) vec))

(defn calcResult [inputValue]
  (digitSum (addUp inputValue)))

(defn getErrStatus [err] ((keyword err) ERR_STATUS_MAP 400))

(defn parseJSON [jsonString]
  (try (json/read-str jsonString :key-fn keyword)
       (catch Exception e (throw (Exception. "ERR_PARSE_JSON")))))

(defn genRes [reqBody]
  (let [inputValue (get-in (parseJSON reqBody) [:address :values])]
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (json/write-str {:result (calcResult inputValue)})}))

(defn genErrRes [err]
  {:status  (getErrStatus err)
   :headers {"Content-Type" "application/json"}
   :body    (json/write-str {:error err})})

(defn app [req]
  (try
    (when-not (entryOf? HTTP_METHODS (:request-method req))
      (throw (Exception. "ERR_HTTP_METHOD")))
    (when-not (entryOf? ACCEPT_CONTENT_TYPES (get-in req [:headers "accept"]))
      (throw (Exception. "ERR_ACCEPT")))
    (when-not (entryOf? REQUEST_CONTENT_TYPES (:content-type req))
      (throw (Exception. "ERR_CONTENT_TYPE")))
    (when-not (> (:content-length req) 0)
      (throw (Exception. "ERR_CONTENT_LENGTH")))
    (genRes (slurp (:body req)))
    (catch Exception e (genErrRes (.getMessage e)))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) PORT_DEFAULT))]
    (run-server app {:port port})
    (println (str "Server started on port " port))))
