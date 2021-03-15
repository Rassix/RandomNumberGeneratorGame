# Random number generator game

### Overview

Task instructions: 

1) The server starts a round of the game and gives 10s to place a bet from the player on numbers from 1 to 10 with the amount of the bet
2) After the time expires, the server generates a random number from 1 to 10
3) If the player guesses the number, a message is sent to him that he won with a winnings of 9.9 times the stake
4) If the player loses receives a message about the loss
5) All players receive a message with a list of winning players in which there is a nickname and the amount of winnings
6) The process is repeated

Application runs game in background where players connect to via websockets and must guess a random number
Application driver is com.rassix.randomNumber.Generator.service.GameScheduler, which coordinates game phases, ends games 
and starts new ones and dictates when to send out notifications. For data storing, game using Java Maps for simplicity. 

** Prerequisites **

* Java 11
* Gradle