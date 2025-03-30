(ns ext
  (:require [google-analytics.browser-ext :as ga]))

(defn init []
  (js/console.log "it's working!")
  (ga/init! "G-YYYYYYYYYY" "YYYYYYYYYYYYYYYYYYYYYY")
  (ga/collect-event "impression"
                    {:name "extension"}))

(init)
