package buildopoli.gameManagement.gameStore

import buildopoli.behaviour.BehaviourModule
import buildopoli.behaviour.BehaviourModule.Behaviour
import buildopoli.gameManagement.gameStore.gameInputs.{GameInputs, UserInputs}
import buildopoli.player.{Player, PlayerImpl}
import buildopoli.terrain.Terrain

import scala.collection.mutable.ListBuffer

private case class GameStoreImpl() extends GameStore:

  private var listOfTerrains: Seq[Terrain] = List()
  private var listOfPLayer: Seq[Player] = List()

  override val userInputs: GameInputs = GameInputs()
  private var _globalBehaviour: Behaviour = Behaviour()

  private var gameStarted: Boolean = false
  private var playerIdsCounter: Int = 0

  def playersList: Seq[Player] = listOfPLayer
  def playersList_=(list: Seq[Player]): Unit = this.listOfPLayer = list

  def terrainList: Seq[Terrain] = listOfTerrains
  def terrainList_=(list: Seq[Terrain]): Unit = this.listOfTerrains = list

  def globalBehaviour_=(behaviour: Behaviour): Unit = _globalBehaviour = behaviour
  def globalBehaviour: Behaviour = _globalBehaviour

  override def getPlayer(playerId: Int): Player = playersList.find(p => p.playerId.equals(playerId)).get
  override def addPlayer(): Unit =
    this.checkGameStarted()
    this.playerIdsCounter += 1
    playersList = playersList :+ PlayerImpl(this.playerIdsCounter)

  override def getTerrain(position: Int): Terrain = terrainList(position)
  override def putTerrain(terrain: Terrain*): Unit =
    this.checkGameStarted()
    terrainList = terrainList ++: terrain

  override def startGame(): Unit =
    this.gameStarted = true
  private def checkGameStarted(): Unit =
    if gameStarted then throw new InterruptedException("Game already started !")
