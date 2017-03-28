(ns aleph-example.http
  (:import
    [io.netty.handler.ssl SslContextBuilder])
  (:require
    [compojure.core :as compojure :refer [GET]]
    [ring.middleware.params :as params]
    [compojure.route :as route]
    [compojure.response :refer [Renderable]]
    [aleph.http :as http]
    [byte-streams :as bs]
    [manifold.stream :as s]
    [manifold.deferred :as d]
    [clojure.core.async :as a]
    [clojure.java.io :refer [file]]))

(defn hello-world-handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "你好世界"})

#_(defn delayed-hello-world-handler
  [req]
  (d/timeout!
    (d/deferred)
    5000
    (hello-world-handler req)))

;; 一个非标准的处理器返回了一个被deferred所包装的Ring返回
;; 在一个典型的Ring兼容服务器中，这可能需要阻塞住一个线程一秒钟来实现
;; 但是用延迟对象允许线程立即返回

;; 这是一个非典型的manifold.deferred/timeout!的使用方式
;; 它会给延迟对象一个超时时间

(extend-protocol Renderable
  manifold.deferred.Deferred
  (render [d _] d))

;; Compojure会解引用并且返回值，但是线程会被锁住。
;; 因为aleph是可以接受一个未被值化的延迟对象，
;; 通过扩展Compojure的渲染协议来传入一个延迟对象来支持异步操作

(defn delayed-hello-world-handler
  [req]
  (s/take!
    (s/->source
      (a/go
        (let [_ (a/<! (a/timeout 5000))]
          (hello-world-handler req))))))

;; 也可以通过使用core.async的goroutine来实现
;; 使用manifold.deferred/->source来转化它并且获取第一个消息
;; 这个跟之前的实现是相同的功能

#_(defn streaming-numbers-handler
  [{:keys [params]}]
  (let [cnt (Integer/parseInt (get params "count" "0"))]
    {:status 200
     :headers {"content-type" "text/plain"}
     :body (->> (range cnt)
                (map #(do (Thread/sleep 100) %))
                (map #(str % "\n")))}))

;; 返回一个stream的HTTP结果，由每个100毫秒产生一个数字组成的字符串
;; 通过Manifold stream来展示典型的惰性序列的样例
;; 跟上面的范例类似，我们不必为每个请求分配线程

;; 这里我们假设字符串值是一个有消息的数字，如果不是Integer.parseInt会产生异常
;; 并且返回一个500状态的堆栈
;; 如果对于状态码更加关注，那么我们可以包装一个try/catch来处理

;; manifold.stream/periodically跟clojure的repeatedly差不多功能，
;; 除了它忽略函数返回的可调整的值

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/hello" [] hello-world-handler)
      (GET "/delay_hello" [] delayed-hello-world-handler)
      (route/not-found "没有相关页面"))))

(def s (http/start-server handler {:port 9930}))

(defn server [] (println "开启服务器"))
