package com.gmail.goosius.siegewar.playeractions;

import org.bukkit.block.Block;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.enums.SiegeStatus;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.utils.SiegeWarDistanceUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarMoneyUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.gmail.goosius.siegewar.settings.Translation;
import com.palmergames.util.TimeMgmt;

/**
 * This class is responsible for processing requests to Abandon siege attacks
 *
 * @author Goosius
 */
public class AbandonAttack {
	
	/**
	 * Process an abandon attack request
	 *
	 * This method does some final checks and if they pass, the abandon is executed
	 *
	 * @param block the banner being placed.
	 * @param nation the one abandoning the siege.
	 * @throws TownyException when the siege cannot be abandoned. 
	 */
    public static void processAbandonSiegeRequest(Block block, Nation nation) throws TownyException {
		// Fail early if the nation has no sieges.
		if (!SiegeController.hasSieges(nation))
			throw new TownyException(Translation.of("msg_err_siege_war_cannot_abandon_nation_not_attacking_zone"));

		//Find the nearest siege to the player, owned by the nation.
		Siege nearestSiege = SiegeWarDistanceUtil.findNearestSiegeForNation(block, nation);
		
		//If there are no nearby siege zones,then regular block request
		if(nearestSiege == null)
			return;
		
        //If the siege is not in progress, send error
		if (nearestSiege.getStatus() != SiegeStatus.IN_PROGRESS)
			throw new TownyException(Translation.of("msg_err_siege_war_cannot_abandon_siege_over"));
		
		// Start abandoning the siege.
		attackerAbandon(nearestSiege);
    	
    }

    private static void attackerAbandon(Siege siege) {
		long timeUntilOfficialAbandon = siege.getTimeUntilAbandonConfirmationMillis();

		if(timeUntilOfficialAbandon > 0) {
			//Pending abandon
			siege.setStatus(SiegeStatus.PENDING_ATTACKER_ABANDON);
			SiegeController.saveSiege(siege);
			Messaging.sendGlobalMessage(
				Translation.of("msg_siege_war_pending_attacker_abandon",
				siege.getAttackingNation().getFormattedName(),
				siege.getDefendingTown().getFormattedName(),
				TimeMgmt.getFormattedTimeValue(timeUntilOfficialAbandon)));
		} else {
			//Immediate abandon
			SiegeWarMoneyUtil.giveWarChestToDefendingTown(siege);
			SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege, SiegeStatus.ATTACKER_ABANDON);
			Messaging.sendGlobalMessage(
				Translation.of("msg_siege_war_attacker_abandon",
				siege.getAttackingNation().getFormattedName(),
				siege.getDefendingTown().getFormattedName()));
		}
	}
}
