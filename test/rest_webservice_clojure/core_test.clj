(ns rest-webservice-clojure.core-test
  (:require [clojure.test :refer [are deftest is testing use-fixtures]]
            [org.httpkit.server :refer [run-server]]
            [clj-http.client :as http]
            [rest-webservice-clojure.core :refer [app]]))

(def PORT 5000)

(defn server-fixture [f]
  (run-server app {:port PORT})
  (f))
(use-fixtures :once server-fixture)

(deftest server-test
  (let [url #(format "http://localhost:%s" %)
        validInput "{\"address\":{\"colorKeys\":[\"A\",\"G\",\"Z\"],\"values\":[74,117,115,116,79,110]},\"meta\":{\"digits\":33,\"processingPattern\":\"d{5}+[a-z&$§]\"}}"
        validResExpected {:status 200 :body "{\"result\":8}"}
        ]
    
    (testing "test invalid http methods"
      (let [fire-req #(% (url PORT) {:accept :json
                                     :content-type :json
                                     :body validInput})
            errRes {:status 405 :body "{\"error\":\"ERR_HTTP_METHOD\"}"}]
        (are [method]
           (= errRes (try (fire-req method)
                          (catch Exception e
                            (select-keys (.getData e) [:status :body]))))
         http/get
         http/put
         http/delete
         http/patch
         http/options)))
    
    (testing "test valid http methods"
      (let [req (http/post (url PORT) {:accept :json
                                       :content-type :json
                                       :body validInput})]
        (is (= validResExpected (select-keys req [:status :body])))))
    
    (testing "test callable paths"
      (are [path]
           (= validResExpected (let [req (http/post (str (url PORT) path) {:accept :json
                                                                           :content-type :json
                                                                           :body validInput})]
                                 (select-keys req [:status :body])))
        "/"
        "/v/e/r/y/l/o/n/g/u/r/l"
        "/with/query?value=1"
        "/\"/"
        "/  /"
        "/´"))
    
    (testing "test undefined as a valid accept content type"
      (let [req (http/post (url PORT) {:accept nil
                                       :content-type :json
                                       :body validInput})]
        (is (= validResExpected (select-keys req [:status :body])))))
    
    (testing "test valid accept content types"
      (are [accept]
           (= validResExpected (let [req (http/post (url PORT) {:accept accept
                                                                :content-type :json
                                                                :body validInput})]
                                 (select-keys req [:status :body])))
        "*/*"
        "application/json"
        :json))
    
    (testing "test invalid accept content types"
      (let [errResExpected {:status 400 :body "{\"error\":\"ERR_ACCEPT\"}"}]
        (are [accept]
             (= errResExpected (let [fire-req #(http/post (url PORT) {:accept %
                                                                      :content-type :json
                                                                      :body validInput})]
                                 (try (fire-req accept)
                                      (catch Exception e
                                        (select-keys (.getData e) [:status :body])))))
          "text/html"
          "text/plain"
          "text/xml"
          "image/jpeg"
          "image/x-icon"
          "video/mp4"
          )))
    
    (testing "test valid request content types"
      (are [contentType]
           (= validResExpected (let [req (http/post (url PORT) {:accept :json
                                                                :content-type contentType
                                                                :body validInput})]
                                 (select-keys req [:status :body])))
        "application/json"
        :json))
    
    (testing "test undefined as an invalid request content type"
      (let [errResExpected {:status 400 :body "{\"error\":\"ERR_CONTENT_TYPE\"}"}]
        (is (= errResExpected (try (http/post (url PORT) {:accept :json
                                                          :content-type nil
                                                          :body validInput})
                                   (catch Exception e
                                     (select-keys (.getData e) [:status :body])))))))

    (testing "test invalid request content types"
     (let [errResExpected {:status 400 :body "{\"error\":\"ERR_CONTENT_TYPE\"}"}]
       (are [contentType]
            (= errResExpected (let [fire-req #(http/post (url PORT) {:accept :json
                                                                     :content-type %
                                                                     :body validInput})]
                                (try (fire-req contentType)
                                     (catch Exception e
                                       (select-keys (.getData e) [:status :body])))))
         "text/html"
         "text/plain"
         "text/xml"
         "image/jpeg"
         "image/x-icon"
         "video/mp4")))
    
    (testing "test empty body as invalid request"
     (let [errResExpected {:status 400 :body "{\"error\":\"ERR_CONTENT_LENGTH\"}"}]
       (are [body]
            (= errResExpected (let [fire-req #(http/post (url PORT) {:accept :json
                                                                     :content-type :json
                                                                     :body %})]
                                (try (fire-req body)
                                     (catch Exception e
                                       (select-keys (.getData e) [:status :body])))))
         nil
         "")))
    
    (testing "test invalid json strings"
     (let [errResExpected {:status 400 :body "{\"error\":\"ERR_PARSE_JSON\"}"}]
       (are [body]
            (= errResExpected (let [fire-req #(http/post (url PORT) {:accept :json
                                                                     :content-type :json
                                                                     :body %})]
                                (try (fire-req body)
                                     (catch Exception e
                                       (select-keys (.getData e) [:status :body])))))
         "invalid"
         "{ invalid: "
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?><invalid number=\"true\">1</invalid>"
         "{invalid:1}"
         "{\"invalid\"=1}")))
          
    (testing "test valid json schemas"
      (are [input resExpected]
           (= resExpected (let [req (http/post (url PORT) {:accept :json
                                                           :content-type :json
                                                           :body input})]
                            (select-keys req [:status :body])))
        "{\"address\":{\"values\":[1,2,3,4]}}" {:status 200 :body "{\"result\":1}"}
        "{\"address\":{\"values\":[]}}" {:status 200 :body "{\"result\":0}"}
        "{\"address\":{\"values\":[1]}}" {:status 200 :body "{\"result\":1}"}
        "{\"address\":{\"values\":[{}]}}" {:status 200 :body "{\"result\":0}"}
        "{\"address\":{\"values\":[{\"valid\":{}}]}}" {:status 200 :body "{\"result\":0}"}
        "{\"address\":{\"values\":[\"valid\"]}}" {:status 200 :body "{\"result\":0}"}
        "{\"address\":{\"values\":[\"valid\",1]}}" {:status 200 :body "{\"result\":1}"}
        "{\"address\":{\"values\":[],\"valid\":1}}" {:status 200 :body "{\"result\":0}"}
        "{\"address\":{\"values\":[]},\"valid\":1}" {:status 200 :body "{\"result\":0}"}))
    
    (testing "test invalid json schemas"
      (let [errResExpected {:status 400 :body "{\"error\":\"ERR_ADDUP_PARAM\"}"}]
        (are [body]
             (= errResExpected (let [fire-req #(http/post (url PORT) {:accept :json
                                                                      :content-type :json
                                                                      :body %})]
                                 (try (fire-req body)
                                      (catch Exception e
                                        (select-keys (.getData e) [:status :body])))))
          "{\"invalid\":1}"
          "{\"address\":1}"
          "{\"address\":{}}"
          "{\"address\":{\"invalid\":1}}"
          "{\"address\":{\"values\":1}}"
          "{\"address\":{\"values\":{}}}")))))
