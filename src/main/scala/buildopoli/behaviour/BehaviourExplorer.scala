package buildopoli.behaviour

import BehaviourModule.*
import buildopoli.behaviour.event.story.EventStoryModule.{EventStory, StoryGroup}
import buildopoli.behaviour.event.EventGroup

/** Allows correctly navigate between event groups and their successors of a Behaviour.
  */
trait BehaviourExplorer extends GenericBehaviourExplorer[Seq[EventGroup]] with StoryConverter:
  /** Index of an event of the behaviour. It is a tuple2: (eventGroupIndex, eventIndex)
    */
  override type Index = (Int, Int)

  /** Triggers selected by index event and update explorer state
    *
    * @param index
    *   is a double tuple where first element is event group index, and second is an event index. Default value (0, 0)
    * @return
    *   a new explorer with updated state
    */
  override def next(index: (Int, Int) = (0, 0)): BehaviourExplorer

  /** @return
    *   empty explorer
    */
  override def endExploring(): BehaviourExplorer

/** Allows extract stories from event groups
  */
trait StoryConverter:
  ex: GenericBehaviourExplorer[Seq[EventGroup]] =>

  /** Get stories of current event groups
    *
    * @return
    *   grouped event stories
    */
  def currentStories: Seq[StoryGroup] =
    ex.currentEvents.map(eventGroup => eventGroup.map(m => m.eventStory(ex.playerId)))

object BehaviourExplorer:
  private type ExplorerStack = List[Seq[EventGroup]]

  /** Constructor of behaviour explorer
    * @param events
    *   the event groups that must be explored
    * @param playerId
    *   id of player that interact with behaviour
    * @return
    *   behaviour explorer that allows to a player to use game events
    */
  def apply(events: Seq[EventGroup], playerId: Int): BehaviourExplorer =
    apply(List(events), playerId)

  private def apply(explorerStateStack: ExplorerStack, playerId: Int): BehaviourExplorer =
    BehaviourExplorerImpl(explorerStateStack, playerId)

  private case class BehaviourExplorerImpl(explorerStateStack: ExplorerStack, override val playerId: Int)
      extends BehaviourExplorer:

    override def hasNext: Boolean = explorerStateStack.nonEmpty && explorerStateStack.head.nonEmpty

    override def next(index: Index): BehaviourExplorer =
      var newStack: ExplorerStack = explorerStateStack.tail
      val groups: Seq[EventGroup] = explorerStateStack.head
      index match
        case (groupIndex: Int, eventIndex: Int)
            if groupIndex < 0 || groupIndex >= groups.length || eventIndex >= groups(groupIndex).length =>
          throw IllegalArgumentException(s"Chose index point to a not existing event. -> $index")

        case (groupIndex: Int, eventIndex: Int) =>
          val newGroup = chooseEvent(groups(groupIndex))(playerId, eventIndex)
          if groups(groupIndex).isAtomic then // event must me explored as atomic
            newStack = groups.patch(groupIndex, Nil, 1) :: newStack
            if newGroup.nonEmpty then newStack = Seq(newGroup.get) :: newStack
          else if newGroup.nonEmpty then // event has a successor that must me added to the current list
            newStack = (newGroup.get +: groups.patch(groupIndex, Nil, 1)) :: newStack
      this.copy(explorerStateStack = newStack)

    override def currentEvents: Seq[EventGroup] = explorerStateStack.head

    override def canEndExploring: Boolean =
      explorerStateStack.size <= 1 && !explorerStateStack.head.exists(_.isMandatory)

    override def endExploring(): BehaviourExplorer = this.copy(explorerStateStack = List())

    private def chooseEvent(eventGroup: EventGroup)(playerId: Int, index: Int): Option[EventGroup] =
      try
        val event = eventGroup(index)
        event.run(playerId)
        if event.nextEvents.nonEmpty then
          val next = event.nextEvents.get
          Some(next.replaceEvents(next.filter(_.hasToRun(playerId))))
        else None
      catch
        case _: IndexOutOfBoundsException =>
          throw IllegalArgumentException("Chose index of a not existing event. -> " + index)
