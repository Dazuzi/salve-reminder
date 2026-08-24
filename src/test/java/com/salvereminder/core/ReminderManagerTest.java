package com.salvereminder.core;
import com.salvereminder.SalveReminderConfig;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.junit.Before;
import org.junit.Test;
import java.util.regex.Pattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
public class ReminderManagerTest {
	private Client client;
	private SalveReminderConfig config;
	private ConfigManager configManager;
	private ReminderManager manager;
	@Before
	public void setUp() {
		client = mock(Client.class);
		config = mock(SalveReminderConfig.class);
		configManager = mock(ConfigManager.class);
		when(config.debugAlert()).thenReturn(SalveReminderConfig.DebugAlert.OFF);
		when(config.ignoredNpcs()).thenReturn("");
		when(config.hideAlertDelay()).thenReturn(2);
		when(config.displayIcon()).thenReturn(SalveReminderConfig.SalveIcon.SALVE_AMULET_EI);
		manager = new ReminderManager(client, config, configManager);
	}
	@Test
	public void dormantTicksDoNoWork() {
		manager.start();
		verify(config, never()).hideAlertDelay();
		clearInvocations(client, config, configManager);
		for (int i = 0; i < 1000; i++) manager.onGameTick();
		verifyNoInteractions(client, config, configManager);
		assertFalse(manager.isShowAlert());
	}
	@Test
	public void unrelatedConfigDoesNotWakeDormantManager() {
		manager.start();
		clearInvocations(client, config, configManager);
		ConfigChanged event = new ConfigChanged();
		event.setGroup(SalveReminderConfig.GROUP);
		event.setKey("backgroundColor");
		manager.onConfigChanged(event);
		verifyNoInteractions(client, config, configManager);
	}
	@Test
	public void initialTargetCaptureRetriesUntilPlayerExists() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(client.getLocalPlayer()).thenReturn(null, player);
		when(player.getInteracting()).thenReturn(npc);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		when(npc.getId()).thenReturn(NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onGameTick();
		assertFalse(manager.isShowAlert());
		manager.onGameTick();
		assertTrue(manager.isShowAlert());
		verify(client, times(2)).getLocalPlayer();
	}
	@Test
	public void npcTransformationInvalidatesStableTarget() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		when(npc.getId()).thenReturn(NpcID.GOBLIN, NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		manager.onGameTick();
		assertFalse(manager.isShowAlert());
		manager.onNpcChanged(new NpcChanged(npc, composition));
		manager.onGameTick();
		assertTrue(manager.isShowAlert());
	}
	@Test
	public void despawnStartsConfiguredHideDelay() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		when(npc.getId()).thenReturn(NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		manager.onGameTick();
		manager.onNpcDespawned(new NpcDespawned(npc));
		manager.onGameTick();
		manager.onGameTick();
		assertTrue(manager.isShowAlert());
		manager.onGameTick();
		assertFalse(manager.isShowAlert());
	}
	@Test
	public void stableTargetIsEvaluatedOnce() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		ItemContainer equipment = mock(ItemContainer.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
		when(npc.getId()).thenReturn(NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		assertTrue(manager.onGameTick());
		assertTrue(manager.isShowAlert());
		clearInvocations(client, config, configManager, player, npc, composition, equipment);
		for (int i = 0; i < 100; i++) assertFalse(manager.onGameTick());
		verifyNoInteractions(client, config, configManager, player, npc, composition, equipment);
	}
	@Test
	public void targetAlertExpiresAfterConfiguredDelay() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		when(npc.getId()).thenReturn(NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		manager.onGameTick();
		manager.onInteractingChanged(new InteractingChanged(player, null));
		manager.onGameTick();
		manager.onGameTick();
		assertTrue(manager.isShowAlert());
		manager.onGameTick();
		assertFalse(manager.isShowAlert());
	}
	@Test
	public void uselessTargetAlertUsesEquippedSalveState() {
		when(config.warnOnUselessSalve()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		ItemContainer equipment = mock(ItemContainer.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx())).thenReturn(new Item(ItemID.CRYSTALSHARD_NECKLACE, 1));
		when(npc.getId()).thenReturn(NpcID.GOBLIN);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Goblin");
		manager.start();
		manager.onItemContainerChanged(new ItemContainerChanged(InventoryID.WORN, equipment));
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		manager.onGameTick();
		assertTrue(manager.isShowAlert());
		assertTrue(manager.isAlertIconCrossed());
		assertEquals("Salve amulet is ineffective against the current target.", manager.getTooltipReason());
	}
	@Test
	public void ignoredWildcardSuppressesTargetAlert() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		when(config.ignoredNpcs()).thenReturn("gh*");
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		when(npc.getId()).thenReturn(NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		manager.onGameTick();
		assertFalse(manager.isShowAlert());
	}
	@Test
	public void ignoredTargetStaysSuppressedWithoutConfigEvent() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		when(npc.getId()).thenReturn(NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		manager.onGameTick();
		assertTrue(manager.isShowAlert());
		manager.ignoreCurrentTarget();
		verify(config).setIgnoredNpcs("Ghost");
		manager.onGameTick();
		assertFalse(manager.isShowAlert());
	}
	@Test
	public void mandatoryTaskAlertUsesEventValueWithoutPolling() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		manager.start();
		ConfigChanged event = new ConfigChanged();
		event.setGroup("slayer");
		event.setKey("taskName");
		event.setNewValue(" Vorkath ");
		manager.onConfigChanged(event);
		assertTrue(manager.isShowAlert());
		assertEquals("Salve amulet is effective for current slayer task.", manager.getTooltipReason());
		verifyNoInteractions(configManager);
	}
	@Test
	public void optionalTaskAlertWorksWithoutTopLevelAlerts() {
		when(config.remindOnBlueDragonsTask()).thenReturn(true);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		manager.start();
		ConfigChanged event = new ConfigChanged();
		event.setGroup("slayer");
		event.setKey("taskName");
		event.setNewValue("blue dragons");
		manager.onConfigChanged(event);
		assertTrue(manager.isShowAlert());
		assertEquals(SpriteID.IconBoss25x25.VORKATH, manager.getAlertSpriteId());
		assertEquals(ItemID.VORKATH_HEAD, manager.getAlertItemId());
	}
	@Test
	public void disablingAllFeaturesReleasesStateAndStopsWork() {
		when(config.alertOnUndeadCombat()).thenReturn(true);
		Player player = mock(Player.class);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(mock(ItemContainer.class));
		when(npc.getId()).thenReturn(NpcID.GHOST);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn("Ghost");
		manager.start();
		manager.onInteractingChanged(new InteractingChanged(player, npc));
		manager.onGameTick();
		assertTrue(manager.isShowAlert());
		when(config.alertOnUndeadCombat()).thenReturn(false);
		ConfigChanged event = new ConfigChanged();
		event.setGroup("salvereminder");
		event.setKey("alertOnUndeadCombat");
		manager.onConfigChanged(event);
		assertFalse(manager.isShowAlert());
		clearInvocations(client, config, configManager, player, npc, composition);
		for (int i = 0; i < 1000; i++) manager.onGameTick();
		verifyNoInteractions(client, config, configManager, player, npc, composition);
	}
	@Test
	public void stackingAppearanceIsReadOnce() {
		when(config.showStackingWarning()).thenReturn(true);
		when(config.hideAlertDelay()).thenReturn(1000);
		when(config.stackingIcon()).thenReturn(SalveReminderConfig.StackingIcon.AUTO);
		ItemContainer equipment = mock(ItemContainer.class);
		when(equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx())).thenReturn(new Item(ItemID.CRYSTALSHARD_NECKLACE, 1));
		when(equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx())).thenReturn(new Item(ItemID.SLAYER_HELM, 1));
		manager.start();
		manager.onItemContainerChanged(new ItemContainerChanged(InventoryID.WORN, equipment));
		clearInvocations(config);
		manager.getAlertItemId();
		for (int i = 0; i < 100; i++) {
			manager.onGameTick();
			manager.getAlertItemId();
		}
		verify(config, times(1)).displayIcon();
		verify(config, times(1)).stackingIcon();
	}
	@Test
	public void wildcardMatchingIsExact() {
		assertTrue(ReminderManager.matchesWildcard("*", "anything"));
		assertTrue(ReminderManager.matchesWildcard("vet*ion", "vet'ion"));
		assertTrue(ReminderManager.matchesWildcard("a**?c", "abbc"));
		assertTrue(ReminderManager.matchesWildcard("?", "x"));
		assertTrue(ReminderManager.matchesWildcard("", ""));
		assertFalse(ReminderManager.matchesWildcard("?", ""));
		assertFalse(ReminderManager.matchesWildcard("a*b", "ac"));
		assertFalse(ReminderManager.matchesWildcard("a", "aa"));
		char[] wildcardAlphabet = {'a', '.', '*', '?'};
		char[] textAlphabet = {'a', '.'};
		for (int wildcardLength = 0; wildcardLength <= 4; wildcardLength++) {
			int wildcardCount = (int) Math.pow(wildcardAlphabet.length, wildcardLength);
			for (int wildcardIndex = 0; wildcardIndex < wildcardCount; wildcardIndex++) {
				String wildcard = indexedString(wildcardIndex, wildcardLength, wildcardAlphabet);
				Pattern expected = Pattern.compile(wildcardRegex(wildcard));
				for (int textLength = 0; textLength <= 4; textLength++) {
					int textCount = (int) Math.pow(textAlphabet.length, textLength);
					for (int textIndex = 0; textIndex < textCount; textIndex++) {
						String text = indexedString(textIndex, textLength, textAlphabet);
						assertEquals(wildcard + ":" + text, expected.matcher(text).matches(), ReminderManager.matchesWildcard(wildcard, text));
					}
				}
			}
		}
	}
	private static String indexedString(int index, int length, char[] alphabet) {
		char[] result = new char[length];
		for (int i = 0; i < length; i++) {
			result[i] = alphabet[index % alphabet.length];
			index /= alphabet.length;
		}
		return new String(result);
	}
	private static String wildcardRegex(String wildcard) {
		StringBuilder regex = new StringBuilder("^");
		for (int i = 0; i < wildcard.length(); i++) {
			char c = wildcard.charAt(i);
			if (c == '*') regex.append(".*");
			else if (c == '?') regex.append('.');
			else {
				if ("\\.[]{}()+-^$|".indexOf(c) != -1) regex.append('\\');
				regex.append(c);
			}
		}
		return regex.append('$').toString();
	}
}
