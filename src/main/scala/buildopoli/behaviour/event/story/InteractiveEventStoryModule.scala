package buildopoli.behaviour.event.story

import buildopoli.behaviour.event.story.EventStoryModule.EventStory

/** Module with all elements of interactive event stories
  */
object InteractiveEventStoryModule:
  /** [[EventStory]] self-type that for each event story choice define an [[Interaction]]
    */
  trait StoryInteraction:
    eventStory: EventStory =>

    /** @return
      *   list of story interactions
      */
    def interactions: Seq[Interaction]

    /** Derivable method that group story choices with interaction
      *
      * @return
      *   grouped story choices with interaction
      */
    def choicesAndInteractions: Seq[(String, Interaction)] =
      for i <- choices.indices
      yield (choices(i), interactions(i))

  /** Event story with story interactions
    */
  trait InteractiveEventStory extends EventStory with StoryInteraction

  /** Result of [[Interaction]]
    */
  enum Result:
    case OK
    case ERR(msg: String)

  /** Event interaction allows to player react to event with some action. Can be used for verify that a player is able
    * to choose some Event. Take in input player id
    */
  type Interaction = Int => Result
