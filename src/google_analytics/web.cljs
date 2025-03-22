(ns google-analytics.web
  "Collecting events in a web app.")

(comment
  (defn gtag [& params]
    (.push (.-dataLayer js/window) (js-arguments))
    (.log js/console "pushing gtag:" (js-arguments)))

  (defn init [measurement-id]
    (let [el (js/document.createElement "script")]
      (.setAttribute el "src" (str "https://www.googletagmanager.com/gtag/js?id="
                                   measurement-id))
      ;; (.setAttribute el "async" true)
      (set! (.-async el) true)
      (.appendChild js/document.head el))

    (set! (.-dataLayer js/window) (or (.-dataLayer js/window) #js []))

    (gtag "js" (js/Date.))
    (gtag "config" "G-YYYYYYYYYY")

    ;; js init
    (comment
      (set! js/window.dataLayer (or js/window.dataLayer
                                    #js []))
      (set! js/window.gtag (fn [& arguments]
                             (.push js/window.dataLayer (clj->js arguments))))
      (js/window.gtag "js" (js/Date.))
      (js/window.gtag "config" measurement-id))
    ))

(comment
  (defn foo [& bar]
    ;; (println arguments)
    (js/console.log "xxx 11" (js-arguments))
    ;; (js/console.log "xxx" (clj->js arguments))
)
  (foo 1 2 3)


  (defn collect [& params]
    (apply js/window.gtag (clj->js params))))

(defn collect-event
  "Collect an event to GA."
  [name params]
  ;; (gtag "event" name params)
  (js/window.gtag "event" name (clj->js params)))
