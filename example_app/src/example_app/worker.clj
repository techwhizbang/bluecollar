(ns example-app.worker)

(defn heavy-lifting [sleep-millis]
  (Thread/sleep sleep-millis)
  (str "That was hard work!!!"))