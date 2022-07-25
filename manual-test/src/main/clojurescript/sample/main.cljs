(ns sample.main
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]))

(defn dispatch-timer-event []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))

(defonce do-timer
  (js/setInterval dispatch-timer-event 1000))

(rf/reg-event-db
 :timer
 (fn [db [_ new-time]]
   (assoc db :time new-time)))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:time (js/Date.)
    :time-color "Orange"}))

(rf/reg-event-db
 :time-color-change
 (fn [db [_ new-color]]
   (assoc db :time-color new-color)))

(rf/reg-sub
 :time
 (fn [db _]
   (:time db)))

(rf/reg-sub
 :time-color
 (fn [db _]
   (:time-color db)))

(defn clock []
  (let [color @(rf/subscribe [:time-color])
        time (-> @(rf/subscribe [:time])
                 .toTimeString
                 (clojure.string/split " ")
                 first)]
    [:div.example-clock {:style {:color color}} time]))

(defn color-input []
  (let [gettext (fn [e] (-> e .-target .-value))
        emit (fn [e] (rf/dispatch [:time-color-change (gettext e)]))]
    [:div.color-input
     "Display color: "
     [:input {:type "text"
              :style {:border "1px solid #CCC"}
              :value @(rf/subscribe [:time-color])
              :on-change emit}]]))

(defn ui []
  [:div
   [:h1 "The time is now"]
   [clock]
   [color-input]])

(defn mount-ui []
  (rdom/render [ui] (js/document.getElementById "app")))

(defn run []
  (rf/dispatch-sync [:initialize])
  (mount-ui))

(js/console.log "I loaded")

(comment
  (run)

  (mount-ui))
