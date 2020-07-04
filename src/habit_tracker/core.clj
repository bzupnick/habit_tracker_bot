(ns habit-tracker.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [next.jdbc :as jdbc]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [next.jdbc.result-set :as rs] )
  (:gen-class))

; TODO: fill correct token
(def token (env :telegram-token))
(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(def help-text "There are 3 commands:
/new <habit name> <unit of measurement>
/log <habit name> <amount accomplished>
/report <habit name> <amount of logs to see>
                
Here are some examples!
/new meditate minutes
/log meditate 30
/log meditate 45
/log meditate 15
/report meditate
                
                    Good luck!")

(def db {:dbtype "postgresql"
            :dbname "habit_tracker"
            :host "localhost"
            :user "postgres"
            :password "postgres"
            :ssl false
            :sslfactory "org.postgresql.ssl.NonValidatingFactory"})

(def ds (jdbc/get-datasource db))

(defn insert-new-habit-to-db
  [habit-name habit-unit telegram-token-id]
  (jdbc/execute-one! ds ["
                          insert into habit(name,unit,telegram_user_token,id)
                          values(?,?,?,?)
                        " habit-name habit-unit telegram-token-id (uuid)] 
                     {:return-keys true}))

(defn habit-name-to-id
  [habit-name telegram-token-id]
  ((jdbc/execute-one! ds ["
                            select id from habit where name=? and telegram_user_token=?"
                            habit-name (str telegram-token-id)
                         ]) :habit/id)
  )

(defn log-having-done-habit
  [habit-name amount telegram-token-id]
  (let [habit-id (habit-name-to-id habit-name telegram-token-id)] 
    (jdbc/execute-one! ds ["
                          insert into log(habit_id,amount,id)
                          values(?,?,?)
                        " habit-id (Integer/parseInt amount) (uuid)] 
                     {:return-keys true})))


(defn new-habit
  [msg]
  (let [user-input (str/split (msg :text) #" " 3)]
    (insert-new-habit-to-db (user-input 1)
                            (user-input 2)
                            ((msg :from) :id))))


(defn log-habit
  [msg]
  (let [user-input (str/split (msg :text) #" " 3)]
    (log-having-done-habit (user-input 1)
                           (user-input 2)
                           ((msg :from) :id))))

(defn log-row->pretty-report
  [log]
  (str "- " (log :habit/name) " for "(log :log/amount) " " (log :habit/unit) "\n"))

(defn prettify-report
  [logs]
  (reduce (fn [final-string log]
            (str final-string (log-row->pretty-report log)))
          ""
          logs))

(defn get-report
  ([telegram-user-token habit-name] (get-report telegram-user-token habit-name 10 :num-of-logs))
  ([telegram-user-token habit-name amount] (get-report telegram-user-token habit-name habit-name amount :num-of-logs))
  ([telegram-user-token habit-name amount unit-of-measurement]
  (let [habit-id (habit-name-to-id habit-name telegram-user-token)]
  (prettify-report (jdbc/execute! ds ["
                            select * from log left join habit on log.habit_id = habit.id where log.habit_id=? LIMIT ?"
                          habit-id amount])))))


(h/defhandler handler
  (h/command-fn "new"
                (fn [msg]
                  (new-habit msg)
                  (t/send-text token ((msg :from) :id) (str "New habit \"" ((str/split (msg :text) #" " 3) 1) "\" created"))))

  (h/command-fn "log"
                (fn [msg]
                  (log-habit msg)
                  (t/send-text token ((msg :from) :id) (str "Logged Habit " ((str/split (msg :text) #" " 3) 1)))))

  (h/command-fn "report"
                (fn [msg]
                  (t/send-text token ((msg :from) :id) (apply get-report ((msg :from) :id) (rest  (str/split (msg :text) #" " 4))))))

  (h/command-fn "start"
                (fn [{{id :id :as chat} :chat}]
                  (t/send-text token id "Welcome to Habit Tracker! Start by checking out /help and happy habit tracking!")))

  (h/command-fn "help"
                (fn [{{id :id :as chat} :chat}]
                  (t/send-text token id help-text)))

  (h/message-fn
   (fn [{{id :id} :chat :as message}]
     (println "Intercepted message: " message)
     (t/send-text token id "I don't do a whole lot ... yet."))))


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the habit_tracker")
  (<!! (p/start token handler)))
