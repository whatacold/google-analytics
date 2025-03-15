(ns google-analytics.browser-ext
  "Collecting events in a browser extension."
  (:require [cljs.core.async :as async]
            [cljs-http.client :as http]))

(def measurement-id (atom nil))
(def api-secret (atom nil))
(def engagement-time-msec 100)
(def session-expiration-min 30)

(defn gen-client-id
  "Generate a new client id."
  []
  (.randomUUID js/self.crypto))

(defn new-session []
  (let [now (-> js/Date .now .toString)]
    {:session_id now
     :timestamp now}))

(defn get-or-create-client-id []
  (let [skey :google-analytics-client-id
        chan (async/chan)]
    ;; TODO only consume one storage key
    (.then (js/chrome.storage.local.get (name skey))
           (fn resolve [items]
             (let [existing (get (js->clj items :keywordize-keys true) skey)
                   new-id (gen-client-id)]
               ;; (js/console.log "xxx" items existing new-id)
               (when-not existing
                 (js/chrome.storage.local.set (clj->js {skey new-id})))
               (async/go
                 (async/>! chan (if existing existing new-id))))))
    chan))

(defn get-or-create-session-id []
  (let [skey :google-analytics-session-data
        chan (async/chan)
        now (.now js/Date)]
    (.then (js/chrome.storage.local.get (name skey))
           (fn [old-data]
             ;; (js/console.log "xxx storage data:" old-data)
             (let [old-data (-> old-data
                                (js->clj :keywordize-keys true)
                                (get skey))
                   old-ts (:timestamp old-data)
                   new-data (if (and old-ts
                                     ;; not expired yet
                                     (<= (/ (- now old-ts)
                                            60000)
                                         session-expiration-min))
                              (assoc old-data :timestamp (.toString now))
                              (new-session))]
               ;; (println "xxx prn" old-data new-data)
               (js/chrome.storage.local.set (clj->js {skey new-data}))
               (async/go (async/>! chan (:session_id new-data))))))
    chan))

(defn mp-endpoint
  ([debug?] (str
             (if debug?
               "https://www.google-analytics.com/debug/mp/collect"
               "https://www.google-analytics.com/mp/collect")
             "?"
             "measurement_id="
             @measurement-id
             "&api_secret="
             @api-secret))
  ([]
   (mp-endpoint false)))

(defn init [measurement-id-arg api-secret-arg]
  (reset! measurement-id measurement-id-arg)
  (reset! api-secret api-secret-arg))

(defn gen-event [name params]
  (async/go
    (let [params (cond-> params
                   (not (:session_id params))
                   (merge {:session_id (async/<! (get-or-create-session-id))})
                   (not (:engagement_time_msec params))
                   (merge {:engagement_time_msec engagement-time-msec}))]
      {:client_id (async/<! (get-or-create-client-id))
       :events [{:name name
                 :params params}]})))

(defn collect-event [name params]
  (when @measurement-id
    (async/go
      (http/post (mp-endpoint)
                 {:headers {"Content-Type" "text/plain;charset=UTF-8"}
                  :json-params (async/<! (gen-event name params))}))))

(comment

  @measurement-id
  (do (init "foo1" "bar1")
      (collect-event "test_click" {:event_name "test"}))
  (gen-client-id)
  (new-session)

  (async/go (let [val (async/<! (get-or-create-client-id))]
              (println "xxx get client id:" val))
            (let [val (async/<! (get-or-create-client-id))]
              (println "xxx get client id:" val))

            (gen-event "test_foo" {:foo 1})

            (let [val (async/<! (get-or-create-session-id))]
              (println "xxx session id:" val))
            (let [val (async/<! (get-or-create-session-id))]
              (println "xxx session id:" val))))
