(ns taoensso.timbre.appenders.3rd-party.monger
  "Monger file appender. Requires MongoDB."
  {:author "Erik Seppanen"}
  (:require [monger.collection :as col]
            [monger.core :as m]
            [monger.credentials :as mcr]
            [monger.db :as mdb]
            [monger.operators :refer :all]
            [monger.query :as q]
            [monger.result :as res]
            [taoensso.timbre :as timbre])
  (:import [com.mongodb MongoClient MongoOptions MongoClientOptions
            ServerAddress WriteConcern]))

(def conn (atom nil))

(def db (atom nil))

(defn ^MongoClient connect
  "Create MongoClient object (the Java MongoDB connection object)."
  [{:keys [host port db coll opts user cred-db pwd]}]
  (let [^MongoClientOptions mopts (m/mongo-options opts)
        ^ServerAddress sa         (m/server-address host port)
        ^MongoCredential cred     (mcr/create user (or cred-db db) pwd)]
    (m/connect sa mopts cred)))

(defn ensure-db [cfg]
  (swap! conn   #(or % (connect cfg)))
  (swap! db #(or % (m/get-db (deref conn) (:db cfg)))))

(defn log-message [data cfg]
  (let [entry {:instant  (str        (:instant       data))
               :level    (str        (:level         data))
               :ns       (str        (:?ns-str       data))
               :hostname (str (force (:hostname_     data)))
               :msg      (str (force (:msg_          data)))
               :err      (str (force (:?err_         data)))}]
    (ensure-db cfg)
    (res/acknowledged? (col/insert (deref db) (:coll cfg) entry))))

(def default-cfg
  {:host "127.0.0.1"
   :port 27017
   :db "timbre-monger-test"
   :coll "logs"
   :opts {:threads-allowed-to-block-for-connection-multiplier 300}
   :user "timbre-monger-test-user"
   :cred-db "admin"
   :pwd "timbre-monger-test-pwd"})

(defn monger-appender
  "Returns a Monger MongoDB appender.
  (monger-appender
    {:host \"127.0.0.1\"
     :port 27017
     :db \"timbre-monger-test\"
     :coll \"logs\"
     :opts {:threads-allowed-to-block-for-connection-multiplier 300}
     :user \"timbre-monger-test-user\"
     :cred-db \"admin\"
     :pwd \"timbre-monger-test\"
    }) "
  [user-cfg]
  (let [cfg (merge default-cfg user-cfg)]
    {:enabled?   true
     :async?     false
     :min-level  nil
     :rate-limit nil
     :output-fn  :inherit
     :fn (fn [data] (log-message data cfg))}))
