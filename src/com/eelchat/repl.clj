(ns com.eelchat.repl
  (:require [com.biffweb :as biff :refer [q]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn get-sys []
  (biff/assoc-db @biff/system))

(defn add-fixtures []
  (biff/submit-tx (get-sys)
    (-> (io/resource "fixtures.edn")
        slurp
        edn/read-string)))

(defn seed-channels []
  (let [{:keys [biff/db] :as sys} (get-sys)]
    (biff/submit-tx sys
                    (for [[mem chan] (q db
                                        '{:find [mem chan]
                                          :where [[mem :mem/comm comm]
                                                  [chan :chan/comm comm]]})]
                      {:db/doc-type :message
                       :msg/mem mem
                       :msg/channel chan
                       :msg/created-at :db/now
                       :msg/text (str "Seed message " (rand-int 1000))}))))
(comment

  ;; As of writing this, calling (biff/refresh) with Conjure causes stdout to
  ;; start going to Vim. fix-print makes sure stdout keeps going to the
  ;; terminal. It may not be necessary in your editor.
  (biff/fix-print (biff/refresh))

  ;; Call this in dev if you'd like to add some seed data to your database. If
  ;; you edit the seed data (in resources/fixtures.edn), you can reset the
  ;; database by running `rm -r storage/xtdb` (DON'T run that in prod),
  ;; restarting your app, and calling add-fixtures again.
  (add-fixtures)

  (let [{:keys [biff/db] :as sys} (get-sys)]
    (q db
       '{:find (pull user [*])
         :where [[user :user/email]]}))

  (sort (keys @biff/system))

  ;; Check the terminal for output.
  (biff/submit-job (get-sys) :echo {:foo "bar"})
  (deref (biff/submit-job-for-result (get-sys) :echo {:foo "bar"}))

  (seed-channels)
  (let [{:keys [biff/db] :as sys} (get-sys)]
    (q db
       '{:find (pull msg [*])
         :where [[msg :msg/txt]]}))
  )

