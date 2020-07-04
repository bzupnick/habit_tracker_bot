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


(h/defhandler handler
  (h/command-fn "new"
                (fn [msg]
                  (new-habit msg)
                  (t/send-text token ((msg :from) :id) (str "New habit \"" ((str/split (msg :text) #" " 3) 1) "\" created"))))

  (h/command-fn "log"
                (fn [msg]
                  (log-habit msg)
                  (t/send-text token ((msg :from) :id) (str "Logged Habit " ((str/split (msg :text) #" " 3) 1)))))

  (h/command-fn "start"
                (fn [{{id :id :as chat} :chat}]
                  (println "Bot joined new chat: " chat)
                  (t/send-text token id "Welcome to habit_tracker!")))

  (h/command-fn "help"
                (fn [{{id :id :as chat} :chat}]
                  (println "Help was requested in " chat)
                  (t/send-text token id "Help is on the way")))

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
