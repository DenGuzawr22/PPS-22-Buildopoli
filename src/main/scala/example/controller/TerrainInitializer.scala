package example.controller

import buildopoli.behaviour.BehaviourModule.Behaviour
import buildopoli.behaviour.event.story.EventStoryModule.EventStory
import buildopoli.behaviour.factory
import buildopoli.behaviour.factory.{BasicBehaviourFactory, BasicEventFactory, BehaviourFactory, EventFactory}
import buildopoli.gameManagement.gameSession.GameSession
import buildopoli.terrain.Mortgage.DividePriceMortgage
import buildopoli.terrain.RentStrategy.RentStrategyPreviousPriceMultiplier
import buildopoli.terrain.card.CardTerrain
import buildopoli.terrain.{Buildable, Purchasable, Terrain, TerrainInfo, Token}

/** Create terrain for one specific game setup
  */
trait TerrainInitializer:
  /** @return
    *   an ordered sequence of game terrains
    */
  def buildGameTerrains(): Seq[Terrain]

object TerrainInitializer:
  def apply(gameSession: GameSession): TerrainInitializer = TerrainInitializerImpl(gameSession)

  private class TerrainInitializerImpl(gameSession: GameSession) extends TerrainInitializer:
    private val eventFactory = factory.EventFactory(gameSession)
    private val behaviourFactory = BehaviourFactory(gameSession)

    override def buildGameTerrains(): Seq[Terrain] =
      var terrains: Seq[Terrain] = Seq()
      val STATION_GROUP = "station"
      val BUILDABLE_GROUP = "buildable"
      terrains = terrains :+ createEmptyTerrain()
      terrains = terrains :+ createSimpleStreet("University street", 100, BUILDABLE_GROUP)
      terrains = terrains :+ createWithdrawMoneyTerrain(50)
      terrains = terrains :+ createTransportStationTerrain("Train station", 300, STATION_GROUP)
      terrains = terrains :+ createWithdrawMoneyTerrain(100)
      terrains = terrains :+ createProbabilityTerrain()
      terrains = terrains :+ createTransportStationTerrain("Bus station", 300, STATION_GROUP)
      terrains = terrains :+ createSurprisesTerrain()
      terrains

    private def createWithdrawMoneyTerrain(amount: Int): Terrain =
      val story = EventStory(s"You spend $amount money on a party", "Oh, noo")
      val behaviour = Behaviour(
        eventFactory.WithdrawMoneyEvent(story, amount)
      )
      Terrain(TerrainInfo("Party"), behaviour)

    private def createEmptyTerrain(): Terrain = Terrain(TerrainInfo("Go"), Behaviour())

    private def createTransportStationTerrain(stationName: String, price: Int, group: String): Terrain =
      val buyStory = EventStory(s"You have an incredible opportunity to buy $stationName", "Buy station")
      val rentStory = EventStory(s"You are at $stationName and must pay for the ticket", "Pay for ticket")
      val errMsg = s"You have not enough money to buy $stationName"
      val behaviour = behaviourFactory.PurchasableTerrainBehaviour(rentStory, errMsg, buyStory)
      Purchasable(
        Terrain(TerrainInfo(stationName), behaviour),
        price,
        group,
        DividePriceMortgage(price, 2),
        RentStrategyPreviousPriceMultiplier(50, 2)
      )

    private def createProbabilityTerrain(): Terrain =
      val t: Terrain = Terrain(TerrainInfo("Probabilities"), Behaviour())
      CardTerrain(t, gameSession, false)

    private def createSurprisesTerrain(): Terrain =
      val t: Terrain = Terrain(TerrainInfo("Surprises"), Behaviour())
      CardTerrain(t, gameSession, true)

    private def createSimpleStreet(streetName: String, price: Int, group: String): Terrain =
      val buyStory = EventStory(s"You can buy terrain on $streetName", "Buy terrain")
      val rentStory = EventStory(s"You ara at $streetName, you must puy rent to the owner", "Pay rent")
      val errMsg = "You have not enough money to pay for the rent"
      val behaviour = behaviourFactory.PurchasableTerrainBehaviour(rentStory, errMsg, buyStory)
      val purchasableTerrain = Purchasable(
        Terrain(TerrainInfo(streetName), behaviour),
        price,
        group,
        DividePriceMortgage(price, 2),
        buildopoli.terrain.RentStrategy.BasicRentStrategyFactor(100, 2)
      )
      val token = Token(Seq("house", "hotel"), Seq(Seq(50, 50), Seq(100)), Seq(25, 50))
      Buildable(purchasableTerrain, token)
