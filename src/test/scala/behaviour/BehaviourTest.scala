package behaviour

import org.scalatest.funsuite.AnyFunSuite
import BehaviourModule.*
import event.EventModule.*
import org.scalatest.*
import util.mock.BankHelper.*
import util.mock.BankHelper.BankMock.*

class BehaviourTest extends AnyFunSuite with BeforeAndAfterEach:

  var bank: BankMock = BankMock()

  override def beforeEach(): Unit =
    bank = BankMock()

  import Scenario.*
  val eventStrategy: () => Unit = () => bank.decrement(TAX)
