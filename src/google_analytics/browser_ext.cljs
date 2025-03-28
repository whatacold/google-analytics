(ns google-analytics.browser-ext
  "Collecting events in a browser extension."
  (:require [cljs.core.async :as async]
            [cljs-http.client :as http]))

(def measurement-id (atom nil))
(def api-secret (atom nil))
(def engagement-time-msec 100)
(def session-expiration-min 30)

(defn- gen-client-id
  "Generate a new client id."
  []
  (.randomUUID js/self.crypto))

(defn- gen-session
  "Generate a new session map."
  []
  (let [now (-> js/Date .now .toString)]
    {:session_id now
     :timestamp now}))

(defn- get-or-gen-client-id
  "Get a client id from Chrome extension's storage, or generate a new one."
  []
  (let [skey :google-analytics-client-id
        chan (async/chan)]
    ;; TODO only consume one storage key
    (.then (js/chrome.storage.local.get (name skey))
           (fn resolve [items]
             (let [old-id (get (js->clj items :keywordize-keys true)
                               skey)
                   new-id (gen-client-id)]
               (when-not old-id
                 (js/chrome.storage.local.set (clj->js {skey new-id})))
               (async/go
                 (async/>! chan (if old-id old-id new-id))))))
    chan))

(defn- get-or-gen-session-id []
  (let [skey :google-analytics-session-data
        chan (async/chan)
        now (.now js/Date)]
    (.then (js/chrome.storage.local.get (name skey))
           (fn [old-data]
             (let [old-data (-> old-data
                                (js->clj :keywordize-keys true)
                                (get skey))
                   old-ts (:timestamp old-data)
                   new-data (if (and old-ts
                                     ;; not expired yet?
                                     (<= (/ (- now old-ts)
                                            60000)
                                         session-expiration-min))
                              (assoc old-data :timestamp (.toString now))
                              (gen-session))]
               (js/chrome.storage.local.set (clj->js {skey new-data}))
               (async/go (async/>! chan (:session_id new-data))))))
    chan))

(defn- ga-mp-endpoint
  "GA measurement protocol endpoint."
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
   (ga-mp-endpoint false)))

(defn- gen-event
  [name params]
  (async/go
    (let [params (cond-> params
                   (not (:session_id params))
                   (merge {:session_id (async/<! (get-or-gen-session-id))})
                   (not (:engagement_time_msec params))
                   (merge {:engagement_time_msec engagement-time-msec}))]
      {:client_id (async/<! (get-or-gen-client-id))
       :events [{:name name
                 :params params}]})))

(defn init!
  "Init with the measurement id and the api secret."
  [measurement-id-arg api-secret-arg]
  (reset! measurement-id measurement-id-arg)
  (reset! api-secret api-secret-arg))

(defn collect-event
  "Collect an event to GA.

  `name` should only contain [a-zA-Z0-9_], `params` is a map."
  [name params]
  (when @measurement-id
    (async/go
      (http/post (ga-mp-endpoint)
                 {:headers {"Content-Type" "text/plain;charset=UTF-8"}
                  :json-params (async/<! (gen-event name params))}))))

(comment
  @measurement-id
  (do (init! "foo1" "bar1")
      (collect-event "test_click" {:event_name "test"}))
  (gen-client-id)
  (gen-session)

  (async/go (let [val (async/<! (get-or-gen-client-id))]
              (println "xxx get client id:" val))
            (let [val (async/<! (get-or-gen-client-id))]
              (println "xxx get client id:" val))

            (gen-event "test_foo" {:foo 1})

            (let [val (async/<! (get-or-gen-session-id))]
              (println "xxx session id:" val))
            (let [val (async/<! (get-or-gen-session-id))]
              (println "xxx session id:" val))))
