package event

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.*
import EventModule.*
import util.mock.BankHelper.*
import util.mock.BankHelper.BankMock.*

class EventTest extends AnyFunSuite with BeforeAndAfterEach:
  val MOCK_ID: Int = 0
  var bank: BankMock = BankMock()
  override def beforeEach(): Unit =
    bank = BankMock()

  test("test fake Bank") {
    assert(bank.money == BANK_MONEY)
    bank.decrement(TAX)
    assert(bank.money == BANK_MONEY - TAX)
    bank.decrement(TAX)
    assert(bank.money == BANK_MONEY - TAX * 2)
  }

  val eventStrategy: EventStrategy = _ => bank.decrement(TAX)
  test("test decrement from strategy") {
    assert(bank.money == BANK_MONEY)
    eventStrategy(MOCK_ID)
    assert(bank.money == BANK_MONEY - TAX)
    eventStrategy(MOCK_ID)
    assert(bank.money == BANK_MONEY - TAX * 2)
  }

  import EventModule.*
  import EventStory.*

  val ev: ConditionalEvent = Event(Scenario(eventStrategy, Scenario.tempStory), _ => true)
  test("I want an event that can change balance of a bank") {
    ev.run(MAIN_ACTION)
    assert(bank.money == BANK_MONEY - TAX)
    ev.run(MAIN_ACTION)
    assert(bank.money == BANK_MONEY - TAX * 2)
  }

  test("I want to have two consecutive events") {
    assert(bank.money == BANK_MONEY)
    import EventOperation.*
    val ev2: ConditionalEvent = Event(Scenario(_ => bank.decrement(TAX * 2), Scenario.tempStory), _ => true) ++ ev
    ev2.run(MAIN_ACTION)
    var nextEv = ev2.nextEvent

    while nextEv.nonEmpty do
      if nextEv.get.eventStory(MOCK_ID).isSingleAction then
        nextEv.get.run(MAIN_ACTION)
        nextEv = nextEv.get.nextEvent
      else fail("Multiple action case is not supported now")
    assert(bank.money == BANK_MONEY - TAX * 3)
  }

  import EventOperation.*
  test("Test ++ operator with Event object") {
    val myEvent: Event = Event(Scenario(Scenario.tempStory))
    val myEvent2: Event = Event(Scenario(Scenario.tempStory))
    val appended = myEvent ++ myEvent2
    assert(appended.isInstanceOf[Event])
    assert(appended.nextEvent.get.isInstanceOf[Event])
  }

  test("Test ++ operator with Conditional object") {
    val myEvent: ConditionalEvent = Event(Scenario(Scenario.tempStory), Event.WITHOUT_PRECONDITION)
    val myEvent2: ConditionalEvent = Event(Scenario(Scenario.tempStory), Event.WITHOUT_PRECONDITION)
    val appended = myEvent ++ myEvent2
    assert(appended.isInstanceOf[ConditionalEvent])
    assert(appended.nextEvent.get.isInstanceOf[ConditionalEvent])
  }

  test("Test ++ with appending an Event to a ConditionalEvent") {
    val condEvent: ConditionalEvent = Event(Scenario(Scenario.tempStory), Event.WITHOUT_PRECONDITION)
    val event: Event = Event(Scenario(Scenario.tempStory))
    assertThrows[IllegalArgumentException](condEvent ++ event)
    assertThrows[IllegalArgumentException](event ++ condEvent)
  }
