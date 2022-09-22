package lib.integration

import lib.behaviour.BehaviourExplorer
import lib.behaviour.BehaviourModule.Behaviour
import lib.behaviour.event.EventFactory
import lib.behaviour.event.EventModule.Event
import lib.behaviour.event.story.EventStoryModule.EventStory
import lib.behaviour.event.story.InteractiveEventStoryModule.{Interaction, InteractiveEventStory, Result}
import lib.behaviour.factory.BehaviourFactory
import lib.gameManagement.gameSession.GameSession
import lib.terrain.Mortgage.*
import lib.terrain.RentStrategy.BasicRentStrategyFactor
import lib.terrain.{Buildable, Purchasable, Terrain, TerrainInfo, Token}
import lib.util.GameSessionHelper
import lib.util.GameSessionHelper.DefaultGameSession
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatest.featurespec.AnyFeatureSpec
import lib.terrain.PurchasableState

import scala.collection.immutable.Seq

class MortgageBehaviourTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterEach:
  private val PLAYER_1 = 1
  private var globalMortgageEvent: Event = _
  private var globalRetrieveMortgageEvent: Event = _
  private val TERRAIN_PRICE = 50
  private val RENT = 50

  private var gameSession: GameSession = _

  override def beforeEach(): Unit =
    gameSession = DefaultGameSession(2)
    val factory = EventFactory(gameSession)

    globalMortgageEvent = factory.MortgageEvent("You can mortgage your terrain and receive money")
    globalRetrieveMortgageEvent = factory.RetrieveFromMortgageEvent(
      "You can retrieve your terrain from mortgage",
      "You have not enough money to retrieve the terrain"
    )

    val buyTerrainStory: EventStory = EventStory("Do you want but this terrain?", "Buy")
    val rentStory: EventStory = EventStory("You need to pay rent", "Pay")
    val behaviourFactory = BehaviourFactory(gameSession)
    val PurchasableBehaviour: Behaviour =
      behaviourFactory.PurchasableTerrainBehaviour(rentStory, "Not enough money", buyTerrainStory)

    val t: Terrain = Terrain(TerrainInfo("Nome1"), PurchasableBehaviour)
    val purchasableTerrain =
      Purchasable(t, TERRAIN_PRICE, "fucsia", DividePriceMortgage(1000, 3), BasicRentStrategyFactor(RENT, 1))

    val token = Token(Seq("house"), Seq(4), Seq(Seq(20, 20, 20, 20)), Seq(25))
    gameSession.gameStore.putTerrain(Buildable(purchasableTerrain, token))
    gameSession.gameStore.globalBehaviour = Behaviour(globalMortgageEvent, globalRetrieveMortgageEvent)
    gameSession.startGame()

  def getFreshExplorer: BehaviourExplorer = gameSession.getFreshBehaviourExplorer(PLAYER_1)

  Feature("Player is able to mortgage his terrains for receive money") {
    Scenario("When terrain is not bought player do not see build mortgage event") {
      Given("combination of global and terrain behaviours")
      var explorer = getFreshExplorer
      assert(explorer.currentEvents.length == 1)

      When("player buy the terrain")
      explorer.next()
      assert(!explorer.hasNext)

      Then("player start to see the global event for mortgage event")
      explorer = getFreshExplorer
      assert(explorer.currentEvents.head.head == globalMortgageEvent)
    }

    Scenario("player buy a terrain and mortgage it") {
      Given("player 1 start its turn")
      var explorer = getFreshExplorer

      When("player buy first terrain")
      assert(runFirstEventStoryInteraction(explorer) == Result.OK)
      explorer.next()

      Then("player mortgage this terrain")
      explorer = getFreshExplorer
      assert(explorer.currentStories.head.head.choices.length == 1)
      assert(runFirstEventStoryInteraction(explorer) == Result.OK)
      explorer.next()

      Then("the state of terrain changed and player receive money")
      assert(gameSession.gameBank.getMoneyOfPlayer(explorer.playerId) > GameSessionHelper.playerInitialMoney)
      assert(
        gameSession.getPlayerTerrain(explorer.playerId).asInstanceOf[Purchasable].state == PurchasableState.MORTGAGED
      )
    }
  }

  Feature("Player is able to retrieve his terrain from mortgage") {
    Scenario("Player put and remove terrain from mortgage") {
      Given("player buy and mortgage a terrain")
      var explorer = getFreshExplorer
      explorer.next()
      explorer = getFreshExplorer
      runFirstEventStoryInteraction(explorer)
      explorer.next()

      When("player check available events")
      explorer = getFreshExplorer

      Then("there is visible only retrieve mortgage event ")
      assert(explorer.currentEvents.length == 1)
      assert(explorer.currentEvents.head.length == 1)
      assert(explorer.currentEvents.head.head == globalRetrieveMortgageEvent)

      When("player has not money")
      gameSession.gameBank.makeTransaction(
        explorer.playerId,
        amount = gameSession.gameBank.getMoneyOfPlayer(explorer.playerId)
      )

      Then("it is not possible to retrieve the terrain")
      assert(runFirstEventStoryInteraction(explorer) match
        case Result.ERR(_) => true
        case Result.OK => false
      )

      When("player have money")
      val aLotOfMoney = 99999
      gameSession.gameBank.makeTransaction(receiverId = explorer.playerId, amount = aLotOfMoney)

      Then("terrain is retrieved successfully")
      assert(runFirstEventStoryInteraction(explorer) == Result.OK)
      explorer.next()
      assert(gameSession.getPlayerTerrain(explorer.playerId).asInstanceOf[Purchasable].state == PurchasableState.OWNED)
      assert(
        gameSession.gameBank.getMoneyOfPlayer(explorer.playerId) == aLotOfMoney - gameSession
          .getPlayerTerrain(explorer.playerId)
          .asInstanceOf[Purchasable]
          .computeMortgage
      )
    }
  }

  private def runFirstEventStoryInteraction(explorer: BehaviourExplorer): Result =
    explorer.currentStories.head.head.asInstanceOf[InteractiveEventStory].interactions.head(explorer.playerId)
