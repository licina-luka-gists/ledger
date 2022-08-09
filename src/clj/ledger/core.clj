(ns ledger.core
  (:require [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.walk])
  (:gen-class))

(s/check-asserts true)
(s/def ::timestamp (s/and int? #(> % (-> 15 (* 1000) (* 1000) (* 100)))))
(s/def ::xe (s/and #(.contains [ "mkd" "rsd" "bam" "hrk" "eur" "xag" "btc" "xmr" ] %)
                   #(= java.lang.String (type %))
                   #(= 3 (count %))))
(s/def ::amt #(-> (read-string %) (or (int?) (float?))))
(s/def ::debit string?)
(s/def ::credit string?)

(s/def ::entry (s/keys :req-un [::timestamp ::xe ::amt ::debit]
                       :opt-un [::credit]))

(defn initialize [fp]
  (if-not (.exists (clojure.java.io/file fp))
    (spit fp "timestamp,xe,amt,debit,credit\n")))

(defn load-ledger [fp]
  (initialize fp)
  (let [data (csv/read-csv (slurp fp))]
    (map zipmap
         (->> (first data)
              (map keyword)
              repeat)
         (rest data))))


(defn xe [sym amt]
  ;; @todo   xe rates need to be in resources/other, not CWD
  ;; @todo   xe rates need to be changeable by substituting the json file
  ;; @todo   dehardcode the values, read a json file
  (let [raw "{\"BAM\":\"1,957507\", \"BTC\":\"0,000044\", \"EUR\":\"1\", \"HRK\":\"7,514552\", \"RSD\":\"117,245237\"}"
        rate (read-string (str/replace (get (json/read-str raw) (str/upper-case sym)) #"," "."))]
    (* amt (/ 1 rate))))

(defn recalc [op prev e]
  (float (op prev (xe (:xe e) (read-string (:amt e))))))

(defn tally [fp]
  (reduce (fn [acc e]
            (let [credited (keyword (:credit e))
                  debited  (keyword (:debit  e))]
              (-> (assoc acc credited (recalc - (credited acc 0) e))
                  (assoc debited (recalc + (debited  acc 0) e)))))
          {}
          (load-ledger fp)))

(defn record! [fp entry]
  (s/assert ::entry entry)
  (let [ line (format "%d,%s,%s,%s,%s\n"
                      (:timestamp entry)
                      (:xe entry)
                      (:amt entry)
                      (:debit entry)
                      (:credit entry "energy")) ]
        (spit fp line :append true))
  (tally fp))

(defn -main [& argv]
  (def fp (format "%s/%s" (System/getProperty "user.home") ".ledger.csv"))
  (def entry (zipmap [:xe :amt :debit :credit] argv))
  (-> (if (empty? entry)
        (tally fp)
        (do
          (->> (quot (System/currentTimeMillis) 1000)
               (assoc entry :timestamp)
               (record! fp))))
      (pp/pprint)))
