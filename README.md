# Tier2
## A staff aid/player mode separation plugin.

### Feature Set
Tier2 is designed for separation between staff and players, allowing server staff to enter an 'assistance' mode to help other players utilising a ticket-based system, and then revert into normal mode to resume playing.

Players are limited to five tickets at any given time. Staff can do an infinite number of tickets. 

While in assistance mode, the following items will be reset upon leaving to their original values:
* Inventory
* Armor
* Location
* Health
* Hunger Level
* Experience

### Commands
* /req
* /mode
* /vanish
* /unvanish
* /check
* /claim
* /tpclaim
* /elevate
* /done


### Permissions
* tier2.staff
 * Staff access.
 * Children:
    * tier2.req
     * Allow a player to submit a help request.
    * tier2.list
     * Allow a player to view a list of staff.
    * tier2.ticket
     * Allow a player to access ticket functionality.
    * tier2.mode
     * Allow a player to toggle between player and assistance mode.
    * tier2.vanish
     * Allow a player to vanish / unvanish.
    * tier2.vanish.see
     * Allow a player to see vanished players.
