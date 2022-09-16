package example.controller

import example.view.{PlayerChoice, View}
import lib.behaviour.event.EventStoryModule.InteractiveEventStory
import lib.gameManagement.gameSession.GameSession
import lib.gameManagement.gameTurn.GameJail
import lib.gameManagement.log.Observer
import lib.behaviour.event.EventStoryModule.Result.*

trait GameController:
  def start(): Unit

class GameControllerImpl(gameSession: GameSession, view: View) extends GameController:
  override def start(): Unit =
    gameSession.startGame()
    val observer: Observer = (msg: String) => view.printLog(msg)
    gameSession.logger.registerObserver(observer)
    
    //todo endgame control
    while true do
      val playerId = gameSession.gameTurn.selectNextPlayer()
      view.showCurrentPlayer(playerId)
      gameSession.movePlayer(playerId)

      val terrain = gameSession.getPlayerTerrain(playerId)
      view.showCurrentTerrain(terrain, gameSession.getPlayerPosition(playerId))
      val behaviourIterator = terrain.getBehaviourIterator(playerId)

      while behaviourIterator.hasNext do
        val stories = behaviourIterator.currentStories
        view.showStoryOptions(stories)
        view.getUserChoices(stories) match
          case PlayerChoice.Choice(groupIdx, eventIdx, choiceIdx)
              if stories(groupIdx)(eventIdx).isInstanceOf[InteractiveEventStory] =>
            stories(groupIdx)(eventIdx).asInstanceOf[InteractiveEventStory].interactions(choiceIdx)(playerId) match
              case OK => behaviourIterator.next((groupIdx, eventIdx))
              case ERR(msg) => view.printLog(msg)
          case PlayerChoice.Choice(groupIdx, eventIdx, _) => behaviourIterator.next((groupIdx, eventIdx))
          case PlayerChoice.EndTurn if behaviourIterator.canEndExploring => behaviourIterator.endExploring()
          case PlayerChoice.EndTurn =>
            view.printLog(s"Player $playerId can not end turn because have to explore mandatory events")