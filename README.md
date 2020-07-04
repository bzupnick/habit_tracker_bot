# Habit Tracker Bot
- [Link to Trello Board](https://trello.com/b/Mb6veQmJ/habit-tracking-bot) that shows development progress

## Prospective Telegram Commands

### Create new habit
- /new <name> <unit of measurement>
    - /new-habit meditate minutes
    - /new-habit brush-teeth minutes
    - /new-habit reading pages

### Log doing a habit
- /log <name> <amount>
    - /log-habit meditate 20
    - /log-habit brush-teeth 60
    - /log-habit reading 15

### Get reports
- /report \<name> <amount=10> \<unit of measurement=num-of-logs>
    - /get-report meditate 
        - Returns the last ten logs
    - /get-report meditate 20
        - Returns the last 20 logs
    - /get-report meditate 20 days
        - Returns the last 20 days worth of logs
    - /get-report meditate days
        - Returns the last 10 days worth of logs
