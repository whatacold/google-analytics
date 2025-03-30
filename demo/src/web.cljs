(ns web
  (:require [google-analytics.web :as ga]
            [reagent.dom :as rd]))

(defn button []
  [:button {:on-click #(ga/collect-event "btn_click"
                                         {:button_name "like"})}
   "click me"])

(defn init []
  (ga/init! "G-YYYYYYYYYY")
  (rd/render
   [:f> button]
   (js/document.querySelector "#root")))
