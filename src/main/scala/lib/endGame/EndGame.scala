package lib.endGame

import lib.gameManagement.gameStore.*
import lib.terrain.{Purchasable, PurchasableState, Terrain}
import lib.player.Player

/** It represents the entity that decides when a player can't play anymore
  */
trait EndGame:

  /** It allows to remove the defeated players from the game
    * @param strategy
    *   that defines when a player is considered defeated
    * @param gameStore
    *   that provides the game information needed
    * @return
    *   the list of the IDs of the defeated players
    */
  def deleteDefeatedPlayer(strategy: Player => Boolean, gameStore: GameStore): Seq[Int]

object EndGame:

  import lib.terrain.TerrainUtils.onlyPurchasable
  import lib.gameManagement.gameBank.Bank

  /** A possible strategy that considers a player defeated if he has no more money and if he does not own any terrain
    * @param player
    *   that we want to check
    * @param gameStore
    *   that provides the information needed
    * @return
    *   a value that says if the player has lost
    */
  def defeatedForNoMoneyAndNoTerrainsOwned(player: Player, gameStore: GameStore, bank: Bank): Boolean =
    bank.debitManagement.getDebitOfPlayer(player.playerId) > 0 ||
      player.getPlayerMoney == 0 && terrainPerPlayer(gameStore.terrainList, player.playerId) == 0

  /** Factory for EndGame implementation that removes the defeated player from the game according to the chosen
    * strategy. It also gives the terrains of the defeated players to the bank
    */
  def apply(): EndGame = NoMoneyNoTerrains()

  private def terrainPerPlayer(terrains: Seq[Terrain], id: Int): Int =
    terrains collect onlyPurchasable count (t => t.state == PurchasableState.OWNED && t.owner.get == id)

  case class NoMoneyNoTerrains() extends EndGame:

    override def deleteDefeatedPlayer(strategy: Player => Boolean, gameStore: GameStore): Seq[Int] =
      val defeatedIDs = gameStore.playersList filter (p => strategy(p)) map (p => p.playerId)
      deleteTerrains(defeatedIDs, gameStore)
      gameStore.playersList = gameStore.playersList filter (p => !strategy(p))
      defeatedIDs

    private def deleteTerrains(ownerIds: Seq[Int], gameStore: GameStore): Unit =
      for t <- gameStore.terrainList collect onlyPurchasable filter (x =>
          x.state != PurchasableState.IN_BANK && (ownerIds contains x.owner.get)
        )
      do t.changeOwner(None)
