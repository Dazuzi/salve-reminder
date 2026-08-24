package com.salvereminder.data;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class SalveDataTest {
	@Test
	public void equipmentIdsRemainComplete() {
		assertTrue(SalveData.isSalveAmulet(ItemID.CRYSTALSHARD_NECKLACE));
		assertTrue(SalveData.isSalveAmulet(ItemID.PVPA_SALVE_AMULET_E));
		assertFalse(SalveData.isSalveAmulet(ItemID.SLAYER_HELM));
		assertTrue(SalveData.isBlackMask(ItemID.HARMLESS_BLACK_MASK_10));
		assertTrue(SalveData.isBlackMask(ItemID.PVPA_SLAYER_HELM_I_ZUK));
		assertTrue(SalveData.isBlackMask(ItemID.LEAGUE_6_SLAYER_HELM1_I));
		assertTrue(SalveData.isBlackMask(ItemID.PVPA_LEAGUE_6_SLAYER_HELM2_I));
		assertFalse(SalveData.isBlackMask(ItemID.CRYSTALSHARD_NECKLACE));
	}
	@Test
	public void targetIdsRemainComplete() {
		assertTrue(SalveData.isUndeadNpc(NpcID.GHOST));
		assertTrue(SalveData.isUndeadNpc(NpcID.VORKATH));
		assertFalse(SalveData.isUndeadNpc(-1));
		assertTrue(SalveData.isUndeadObject(ObjectID.POH_COMBAT_DUMMY_UNDEADSLAYER));
		assertFalse(SalveData.isUndeadObject(-1));
	}
	@Test
	public void mandatoryTaskLookupIsExact() {
		assertTrue(SalveData.isMandatorySlayerTask("aberrant spectres"));
		assertTrue(SalveData.isMandatorySlayerTask("vorkath"));
		assertFalse(SalveData.isMandatorySlayerTask("blue dragons"));
		assertFalse(SalveData.isMandatorySlayerTask("Vorkath"));
	}
}
