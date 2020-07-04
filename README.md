# Habit Tracker Bot
- [Link to Trello Board](https://trello.com/b/Mb6veQmJ/habit-tracking-bot) that shows development progress

## Prospective Telegram Commands

### Create new habit
- /new \<name> \<unit of measurement>
    - /new meditate minutes
    - /new brush-teeth minutes
    - /new reading pages

### Log doing a habit
- /log \<name> \<amount>
    - /log meditate 20
    - /log brush-teeth 60
    - /log reading 15

### Get reports
- /report \<name> <amount=10> \<unit of measurement=num-of-logs>
    - /report meditate 
        - Returns the last ten logs
    - /report meditate 20
        - Returns the last 20 logs
    - /report meditate 20 days
        - Returns the last 20 days worth of logs
    - /report meditate days
        - Returns the last 10 days worth of logs
