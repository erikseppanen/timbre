(ns taoensso.timbre.appenders.3rd-party.monger-test
  (:require [clojure.test :refer :all]
            [monger.collection :as col]
            [monger.core :as m]
            [monger.db :as mdb]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.monger :as monger]))

(def cfg {:host "127.0.0.1"
           :port 27017
           :db "timbre-monger-test"
           :coll "logs"
           :opts {:threads-allowed-to-block-for-connection-multiplier 300}
           :user "timbre-monger-test-user"
           :cred-db "admin"
           :pwd "timbre-monger-test-pwd"})

(def conn (m/connect))
(def db (m/get-db conn (:db cfg)))

(defn setup [f]
  ;; Add admin test user for all tests.
  ;; Tests will authenticate to MongoDB using this.
  (mdb/add-user db (:user cfg) (char-array (:pwd cfg)))
  (f))

(defn teardown [f]
  ;; Drop test DB before next test run.
  (mdb/drop-db db)
  (f))

(use-fixtures :once setup)
(use-fixtures :each teardown)

(deftest monger-test []
  (do
    (timbre/merge-config! {:appenders {:monger (monger/monger-appender cfg)}})
    (timbre/info "1234"))
  ;; (let [m (col/find-one-as-map (monger/db) (monger/conn) {:msg "1234"})]
  ;;   m
  ;;   ;; (is (= (:status response) 200))
  ;;   ;; (is (= (some #(= % (->comparable es)) (strip-body (:body response)))
  ;;   ;;        true))
  ;;   )
  )
