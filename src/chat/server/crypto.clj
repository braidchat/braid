(ns chat.server.crypto
  (:import javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec))

(defn hmac
  [hmac-key data]
  (let [key-bytes (.getBytes hmac-key "UTF-8")
        data-bytes (.getBytes data "UTF-8")
        algo "HmacSHA256"]
    (->>
      (doto (Mac/getInstance algo)
        (.init (SecretKeySpec. key-bytes algo)))
      (#(.doFinal % data-bytes))
      (map (partial format "%02x"))
      (apply str))))

(defn constant-comp
  "Compare two strings in constant time"
  [a b]
  (loop [a a b b match (= (count a) (count b))]
    (if (and (empty? a) (empty? b))
      match
      (recur
        (rest a)
        (rest b)
        (and match (= (first a) (first b)))))))
