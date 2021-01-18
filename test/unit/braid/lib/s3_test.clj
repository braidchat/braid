(ns unit.braid.lib.s3-test
  (:require
   [braid.lib.s3 :refer :all]
   [braid.lib.aws :as aws]
   [org.httpkit.client :as http]
   [clojure.test :refer :all]))

(def config {:aws-region  "aws-region"
             :aws-bucket "aws-bucket"
             :aws/credentials-provider (constantly ["aws-access-key" "aws-secret-key"])})

(deftest can-generate-s3-host
  (is (= "aws-bucket.s3.aws-region.amazonaws.com" (s3-host config))))

(deftest can-generate-s3-upload-policy
  (let [expected {:bucket "aws-bucket"
                  :region "aws-region"
                  :auth {:policy "eyJleHBpcmF0aW9uIjoiMjAyMS0wMS0wMVQwMDowNTowMFoiLCJjb25kaXRpb25zIjpbeyJidWNrZXQiOiJhd3MtYnVja2V0In0sWyJzdGFydHMtd2l0aCIsIiRrZXkiLCIiXSx7ImFjbCI6InByaXZhdGUifSxbInN0YXJ0cy13aXRoIiwiJENvbnRlbnQtVHlwZSIsIiJdLFsiY29udGVudC1sZW5ndGgtcmFuZ2UiLDAsNTI0Mjg4MDAwXSx7IngtYW16LWFsZ29yaXRobSI6IkFXUzQtSE1BQy1TSEEyNTYifSx7IngtYW16LWNyZWRlbnRpYWwiOiJhd3MtYWNjZXNzLWtleVwvMjAyMTAxMDFcL2F3cy1yZWdpb25cL3MzXC9hd3M0X3JlcXVlc3QifSx7IngtYW16LWRhdGUiOiIyMDIxMDEwMVQwMDAwMDBaIn1dfQ=="
                         :key "aws-access-key"
                         :signature "d3230795fc4d3ffbd3387d9eb02c00250121361bb7e9a1ea68eb347a630f2634"
                         :credential "aws-access-key/20210101/aws-region/s3/aws4_request"
                         :date "20210101T000000Z"
                         :security-token nil}}]
    (with-redefs [aws/utc-now (constantly (java.time.ZonedDateTime/parse "2021-01-01T00:00:00Z"))]
      (is (= expected (generate-s3-upload-policy config {:starts-with ""}))))))

(deftest can-build-readable-s3-url
  (let [expected "https://aws-bucket.s3.aws-region.amazonaws.com/dijkstra?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=aws-access-key%2F20210101%2Faws-region%2Fs3%2Faws4_request&X-Amz-Date=20210101T000000Z&X-Amz-Expires=10000&X-Amz-SignedHeaders=host&X-Amz-Signature=a8f771795c835fd5f7ca75fb8ce54385cb8f591514b91d34cb34c7679fc45416"]
    (with-redefs [aws/utc-now (constantly (java.time.ZonedDateTime/parse "2021-01-01T00:00:00Z"))]
      (is (= expected (readable-s3-url config 10000 "dijkstra"))))))

(deftest can-delete-file
  (with-redefs [http/delete (fn [& _] (atom :irrelevant))]
    (is (= :irrelevant (delete-file! config "/my-bucket/my-key")))))

(deftest can-test-upload-url-path
  (is (upload-url-path "https://aws-bucket.s3.aws-region.amazonaws.com/dijkstra"))
  (is (not (upload-url-path "https://aws-bucket.s3.aws-region.something.com/dijkstra"))))
