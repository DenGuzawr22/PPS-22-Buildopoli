package behaviour.event

import behaviour.event.*
import EventModule.*
import EventStoryModule.*
import Event.*
import gameManagement.gameSession.GameSession
import org.slf4j.{Logger, LoggerFactory}

object EventFactory:
  type EventLogMsg = String => String

  def apply(gameSession: GameSession): StandardEventFactory = EventFactoryImpl(gameSession)

  def InfoEvent(story: EventStory, condition: EventPrecondition): Event =
    Event(Scenario(story), condition)

  class EventFactoryImpl(gameSession: GameSession) extends StandardEventFactory:
    val logger: Logger = LoggerFactory.getLogger(this.getClass)
    private val gameTurn = gameSession.gameTurn
    private val dice = gameSession.dice

    override def ImprisonEvent(story: EventStory, blockingTime: Int): Event =
      val imprisonStrategy: Int => Unit = playerId =>
        gameTurn.getRemainingBlockedMovements(playerId) match
          case None =>
            gameTurn.lockPlayer(playerId, blockingTime)
          case _ =>
      Event(Scenario(imprisonStrategy, story))

    override def EscapeEvent(story: EventStory, escapeSuccessMsg: EventLogMsg, escapeFailMsg: EventLogMsg): Event =
      val escapeStrategy: Int => Unit = playerId =>
        if dice.rollOneDice() == dice.rollOneDice() then
          gameTurn.liberatePlayer(playerId)
          logger.info(escapeSuccessMsg(playerId.toString))
          gameSession.setPlayerPosition(playerId, dice.rollMoreDice(2), true)
        else logger.info(escapeFailMsg(playerId.toString))
      val escapePrecondition: EventPrecondition = gameTurn.getRemainingBlockedMovements(_).nonEmpty
      Event(Scenario(escapeStrategy, story), escapePrecondition)
