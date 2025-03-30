(ns google-analytics.web
  "Collecting events in a web app.")

(defn init! [measurement-id]
  (let [src-el (js/document.createElement "script")]
    (.setAttribute src-el "src" (str "https://www.googletagmanager.com/gtag/js?id="
                                 measurement-id))
    (set! (.-async src-el) true)
    (.appendChild js/document.head src-el))
  (let [init-el (js/document.createElement "script")]
    (set! (.-innerHTML init-el)
          (str "
window.dataLayer = window.dataLayer || [];
function gtag() { dataLayer.push(arguments); }
  gtag('js', new Date());
  gtag('config', '" measurement-id "');
"))
    (.appendChild js/document.head init-el)))

(defn collect-event
  "Collect an event to GA.

  `name` should only contain [a-zA-Z0-9_], `params` is a map."
  [name params]
  ;; (gtag "event" name params)
  (js/window.gtag "event" name (clj->js params)))
