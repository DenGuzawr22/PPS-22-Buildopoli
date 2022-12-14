package buildopoli.gameManagement.gameSession

import buildopoli.gameManagement.gameBank.{Bank, GameBankImpl}
import org.scalatest.funsuite.AnyFunSuite
import buildopoli.lap.Lap.MoneyReward
import buildopoli.gameManagement.gameOptions.GameOptions
import buildopoli.gameManagement.gameSession.GameSession
import buildopoli.gameManagement.gameStore.GameStore
import buildopoli.gameManagement.gameTurn.{DefaultGameTurn, GameTurn}
import buildopoli.lap.Lap
import buildopoli.player.{Player, PlayerImpl}
import buildopoli.terrain.Mortgage.DividePriceMortgage
import buildopoli.terrain.RentStrategy.RentStrategyWithBonus
import buildopoli.terrain.{Purchasable, Terrain, TerrainInfo}
import org.scalatest.BeforeAndAfterEach

import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer

class GameSessionTest extends AnyFunSuite with BeforeAndAfterEach:

  var gameSession: GameSession = _
  var gameStore: GameStore = _

  override def beforeEach(): Unit =
    val selector: (Seq[Player], Seq[Int]) => Int =
      (playerList: Seq[Player], playerWithTurn: Seq[Int]) =>
        playerList.filter(el => !playerWithTurn.contains(el.playerId)).head.playerId
    gameStore = GameStore()
    val gameOptions: GameOptions = GameOptions(200, 2, 10, 6, selector)
    val gameBank: Bank = GameBankImpl(gameStore)
    val gameTurn: GameTurn = GameTurn(gameOptions, gameStore)
    val gameLap: Lap = Lap(MoneyReward(200, gameBank))
    gameSession = GameSession(gameOptions, gameBank, gameTurn, gameStore, gameLap)
    for i <- 0 until 20 do
      val t: Terrain = Terrain(TerrainInfo("terreno" + i), null)
      val p: Purchasable = Purchasable(t, 1000, null, DividePriceMortgage(1000, 3), RentStrategyWithBonus(50, 20))
      gameSession.gameStore.putTerrain(p)

  test("playersList has initial size at zero") {
    assert(gameStore.playersList.size === 0)
  }

  test("There should be 10 users inside playersList") {
    this.gameSession.startGame()
    assert(gameStore.playersList.size === 10)
    assert(gameSession.gameBank.gameStore.playersList.size === 10)
    assert(gameSession.gameStore.playersList.size === 10)
  }

  test("All players created inside playersList are initialized with 200 money") {
    this.gameSession.startGame()
    assert(gameStore.playersList.size === 10)
    gameSession.gameStore.playersList.foreach(pl => assert(pl.getPlayerMoney === 200))
  }

  test("giving one terrain to each user at start game") {
    this.gameSession.startGame()

    gameSession.gameStore.playersList.foreach(pl =>
      assert(
        gameSession.gameStore.getNumberOfTerrains(tr =>
          tr.isInstanceOf[Purchasable] &&
            tr.asInstanceOf[Purchasable].owner === Option.apply(pl.playerId)
        ) == 2
      )
    )
  }
