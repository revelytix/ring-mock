(ns ring.mock.test.request
  (:use clojure.test
        ring.mock.request)
  (:require [clojure.java.io :as io])
  (:import [java.io InputStream StringWriter]))

(deftest test-request
  (testing "relative uri"
    (is (= (request :get "/foo")
           {:server-port 80
            :server-name "localhost"
            :remote-addr "localhost"
            :uri "/foo"
            :query-string nil
            :scheme :http
            :request-method :get
            :headers {"host" "localhost"}})))
  (testing "absolute uri"
    (is (= (request :post "https://example.com:8443/foo?bar=baz")
           {:server-port 8443
            :server-name "example.com"
            :remote-addr "localhost"
            :uri "/foo"
            :query-string "bar=baz"
            :scheme :https
            :request-method :post
            :headers {"host" "example.com:8443"}})))
  (testing "nil path"
    (is (= (:uri (request :get "http://example.com")) "/")))
  (testing "added params"
    (is (= (:query-string (request :get "/" {:x "y" :z "n"}))
           "x=y&z=n"))
    (is (= (:query-string (request :get "/?a=b" {:x "y"}))
           "a=b&x=y"))
    (is (= (:query-string (request :get "/?" {:x "y"}))
           "x=y"))
    (is (= (:query-string (request :get "/" {:x "a b"}))
           "x=a+b"))))

(deftest test-header
  (is (= (header {} "X-Foo" "Bar")
         {:headers {"x-foo" "Bar"}}))
  (is (= (header {} :x-foo "Bar")
         {:headers {"x-foo" "Bar"}})))

(deftest test-content-type
  (is (= (content-type {} "text/html")
         {:content-type "text/html"
          :headers {"content-type" "text/html"}})))

(deftest test-content-length
  (is (= (content-length {} 10)
         {:content-length 10
          :headers {"content-length" "10"}})))

(defn- slurp* [stream]
  (let [writer (StringWriter.)]
    (io/copy stream writer)
    (str writer)))

(deftest test-body
  (testing "string body"
    (let [resp (body {} "Hello World")]
      (is (instance? InputStream (:body resp)))
      (is (= (slurp* (:body resp)) "Hello World"))
      (is (= (:content-length resp) 11))))
  (testing "map body"
    (let [resp (body {} {:foo "bar"})]
      (is (instance? InputStream (:body resp)))
      (is (= (slurp* (:body resp)) "foo=bar"))
      (is (= (:content-length resp) 7))
      (is (= (:content-type resp)
             "application/x-www-form-urlencoded")))))
