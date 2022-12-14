package buildopoli.terrain.card

import buildopoli.behaviour.BehaviourExplorer
import buildopoli.behaviour.event.EventModule.{Event, EventStrategy}
import buildopoli.behaviour.event.story.EventStoryModule.EventStory
import buildopoli.behaviour.event.EventGroup
import buildopoli.behaviour.event.story.EventStoryModule.EventStory
import buildopoli.gameManagement.gameSession.GameSession
import buildopoli.gameManagement.gameTurn.GameJail
import buildopoli.terrain.Terrain
import buildopoli.terrain.*

/** Representing the card terrain. Each terrain has a list of cards and some methods to manage them When player arrives
  * and calls getBehaviourExplorer, consequences of one sorted card are applied
  */
trait CardTerrain extends Terrain:

  /** List of cards for each terrain
    */
  var cardList: List[Card]

  /** To add cards to the previous list
    * @param card
    *   a new Card (surprise or probability, typically)
    */
  def addCards(card: Card*): Unit

  /** To remove a specific card from the list
    * @param name
    *   identifying one card
    */
  def removeCard(name: String): Unit

object CardTerrain:

  /** Factory to create a terrain, without cards pre created
    * @param terrain
    *   instance of terrain where to put the card terrain
    * @return
    *   a new DefaultCardTerrain
    */
  def apply(terrain: Terrain): CardTerrain = DefaultCardTerrain(terrain: Terrain)

  /** Factory to create a terrain, without some surprise or probability cards pre-installed
    * @param terrain
    *   instance of terrain where to put the card terrain
    * @param gameSession
    *   current instance of gameSession to use when defining event groups and consequences of each card
    * @param surprises
    *   boolean object, if true some surprise cards will be added, else some probability cards will be added
    * @return
    *   a new DefaultCardTerrain with some probability or surprise cards
    */
  def apply(terrain: Terrain, gameSession: GameSession, surprises: Boolean): CardTerrain =
    val defaultTerrain = DefaultCardTerrain(terrain)
    surprises match
      case true => defaultTerrain.cardList = createSurpriseCards(gameSession)
      case false => defaultTerrain.cardList = createProbabilityCards(gameSession)
    defaultTerrain

  /** To create some surprise cards to be added in a CardTerrain
    * @param gameSession
    *   used to create consequences of each card
    * @return
    *   list of surprise cards
    */
  def createSurpriseCards(gameSession: GameSession): List[Card] =
    val giveMoneyAllStory: EventStory = EventStory("Test", "Give 500 money to all others")
    val giveMoneyAllStrategy: EventStrategy = id =>
      gameSession.gameBank.makeGlobalTransaction(senderId = id, amount = 500)
    val giveMoneyAll = DefaultCards(EventGroup(Event(giveMoneyAllStory, giveMoneyAllStrategy)), "give money all")

    val doOneLapWithoutRewardStory: EventStory =
      EventStory("Test", "Do One Lap and stop at the start cell without reward")
    val doOneLapWithoutRewardStrategy: EventStrategy = id =>
      gameSession.movePlayer(
        id,
        steps = (gameSession.gameStore.getNumberOfTerrains(_ => true) - gameSession.getPlayerPosition(id)) + 1
      )
    val doOneLapWithoutReward = DefaultCards(
      EventGroup(Event(doOneLapWithoutRewardStory, doOneLapWithoutRewardStrategy)),
      "do one lap without reward"
    )

    List(giveMoneyAll, doOneLapWithoutReward)

  /** To create some probability cards to be added in a CardTerrain
    * @param gameSession
    *   used to create consequences of each card
    * @return
    *   list of probability cards
    */
  def createProbabilityCards(gameSession: GameSession): List[Card] =
    val removeMoneyStory: EventStory = EventStory("Test", "Remove 500 money")
    val removeMoneyStrategy: EventStrategy = id => gameSession.gameBank.makeTransaction(id, amount = 500)
    val removeMoney = DefaultCards(EventGroup(Event(removeMoneyStory, removeMoneyStrategy)), "remove money")

    val addMoneyStory: EventStory = EventStory("Test", "Add 500 money")
    val addMoneyStrategy: EventStrategy = id => gameSession.gameBank.makeTransaction(receiverId = id, 500)
    val addMoney = DefaultCards(EventGroup(Event(addMoneyStory, addMoneyStrategy)), "add money")

    val doOneLapStory: EventStory = EventStory("Test", "Do One Lap and stop at the start cell")
    val doOneLapStrategy: EventStrategy = id =>
      gameSession.movePlayer(
        id,
        steps = (gameSession.gameStore.getNumberOfTerrains(_ => true) - gameSession.getPlayerPosition(id)) + 1
      )
    val doOneLap = DefaultCards(EventGroup(Event(doOneLapStory, doOneLapStrategy)), "do one lap")

    List(removeMoney, addMoney, doOneLap)
