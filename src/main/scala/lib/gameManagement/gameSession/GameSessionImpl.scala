package lib.gameManagement.gameSession

import lib.gameManagement.diceGenerator.{Dice, SingleDice}
import lib.gameManagement.gameBank.{Bank, GameBankImpl}
import lib.gameManagement.gameOptions.GameOptions
import lib.gameManagement.gameStore.GameStore
import lib.gameManagement.gameTurn.GameTurn
import lib.lap.Lap
import lib.player.{Player, PlayerImpl}
import lib.terrain.{Buildable, GroupManager, Purchasable, PurchasableState, Terrain}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class GameSessionImpl(
    override val gameOptions: GameOptions,
    override val gameBank: Bank,
    override val gameTurn: GameTurn,
    override val gameStore: GameStore,
    override val gameLap: Lap
) extends GameSession:

  override val dice: Dice = SingleDice(gameOptions.diceFaces)
  override val logger: Logger = LoggerFactory.getLogger("GameSession")
  private var groupManager: GroupManager = _

  override def getGroupManager: GroupManager = this.groupManager

  override def startGame(): Unit =
    this.addManyPlayers(gameOptions.nUsers)
    this.gameStore.startGame()
    initializePlayers()
    this.groupManager = GroupManager(this.gameStore.terrainList)

  private def addManyPlayers(n: Int): Unit =
    for _ <- 0 until n do this.gameStore.addPlayer()

  private def initializePlayers(): Unit = this.enoughPurchasableTerrains() match
    case true =>
      this.gameStore.playersList.foreach(pl =>
        pl.setPlayerMoney(gameOptions.playerInitialMoney)
        this.assignTerrains(pl.playerId)
      )
    case _ => throw new IllegalStateException("Not enough terrains !")

  private def assignTerrains(playerId: Int): Unit =
    for _ <- 0 until this.gameOptions.playerInitialCells do
      this.getRandomPurchasableTerrainWithoutOwner
        .asInstanceOf[Purchasable]
        .changeOwner(Some(playerId))

  private def getRandomPurchasableTerrainWithoutOwner: Terrain =
    val purchasableTerrainList: Seq[Terrain] =
      this.gameStore.getTypeOfTerrains(tr =>
        tr.isInstanceOf[Purchasable]
          && tr.asInstanceOf[Purchasable].owner.isEmpty
      )
    purchasableTerrainList(Random.nextInt(purchasableTerrainList.size))

  def enoughPurchasableTerrains(): Boolean =
    this.gameStore.getNumberOfTerrains(tr =>
      tr.isInstanceOf[Purchasable]
    ) >= this.gameOptions.nUsers * this.gameOptions.playerInitialCells

  override def setPlayerPosition(playerId: Int, nSteps: Int, isValidLap: Boolean): Unit =
    val player = gameStore.getPlayer(playerId)
    val result =
      gameLap.isNewLap(isValidLap, player.getPlayerPawnPosition, nSteps, gameStore.getNumberOfTerrains(_ => true))
    player.setPlayerPawnPosition(result._1)
    if result._2 then gameLap.giveReward(playerId)
