(ns dev
  (:require [nrepl.transport :as t]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.misc :refer [response-for]])
  (:import [java.time LocalDate]))

(defn silly-handler [{:keys [code transport] :as msg}]
  (t/send transport (response-for msg :status :done :value (pr-str "Keep trying!"))))

(defn current-date [h]
  (fn [{:keys [op transport] :as msg}]
    (if (= "now" op)
      (t/send transport (response-for msg :status :done :value (str (LocalDate/now))))
      (h msg))))

(set-descriptor! #'current-date
  {:handles {"now" {:doc "Returns the current date."
                    :returns {"value" "The current date as an ISO-8601 string."}}}})

(defn number-1 [h]
  (fn [{:keys [op transport] :as msg}]
    (if (= "num1" op)
      (t/send transport (response-for msg :status :done :value "one"))
      (h msg))))

(set-descriptor! #'number-1
  {:handles {"num1" {:doc "Returns the number 1."}
                    :returns {"value" "The number 1."}}})
