package buildopoli.gameManagement.gameStore.gameInputs

import buildopoli.gameManagement.gameOptions.GameOptions
import buildopoli.gameManagement.gameStore.gameInputs.{GameInputs, UserInputs}
import buildopoli.gameManagement.gameStore.GameStore
import buildopoli.gameManagement.gameTurn.{DefaultGameTurn, GameTurn}
import buildopoli.player.{Player, PlayerImpl}
import org.scalatest.funsuite.AnyFunSuite

class GameInputsTest extends AnyFunSuite:
  val userInput: GameInputs = UserInputs()

  val selector: (Seq[Player], Seq[Int]) => Int =
    (playerList: Seq[Player], playerWithTurn: Seq[Int]) =>
      playerList.filter(el => !playerWithTurn.contains(el.playerId)).head.playerId

  val gameStore: GameStore = GameStore()
  val gameOptions: GameOptions = GameOptions(200, 2, 10, 6, selector)
  val gameTurn: GameTurn = GameTurn(gameOptions, gameStore)

  test("adding one tail element") {
    userInput.addTailInputEvent("tailElement")
    assert(!userInput.isListEmpty)
    assert(userInput.getHeadElement === "tailElement")
  }

  test("removing the head element, adding one tail element") {
    userInput.addTailInputEvent("secondTailElement")
    assert(!userInput.isListEmpty)
    assert(userInput.getHeadElement === "tailElement")
    userInput.removeHeadElement()
    assert(userInput.getHeadElement === "secondTailElement")
  }

  test("assert gameInput inside gameStore is empty") {
    assert(gameStore.userInputs.isListEmpty)
  }

  test("adding two players and testing turns with list not empty") {
    gameStore.addPlayer()
    gameStore.addPlayer()
    assert(gameTurn.selectNextPlayer() === 1)
    assert(gameTurn.playerWithTurn.head === 1)
    gameStore.userInputs.addTailInputEvent("inputElement")
    assertThrows[RuntimeException](gameTurn.selectNextPlayer() === 2)
  }

  test("emptying list should allow to proceed with turns") {
    gameStore.userInputs.removeHeadElement()
    assert(gameStore.userInputs.isListEmpty)
    assert(gameTurn.selectNextPlayer() === 2)
    assert(gameTurn.playerWithTurn.size == 2)
  }
