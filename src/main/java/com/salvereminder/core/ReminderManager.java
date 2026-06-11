package com.salvereminder.core;
import com.salvereminder.SalveReminderConfig;
import com.salvereminder.data.SalveData;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
@Singleton
public class ReminderManager {
	private static final String SLAYER_PLUGIN_GROUP = "slayer";
	private static final String SLAYER_TASK_NAME_KEY = "taskName";
	@Inject
	private Client client;
	@Inject
	private SalveReminderConfig config;
	@Inject
	private ConfigManager configManager;
	@Getter
	private boolean showAlert = false;
	@Getter
	private boolean flash = false;
	@Getter
	private String tooltipReason = null;
	@Getter
	private int conflictingHeadgearId = -1;
	@Getter
	private boolean isStackingWarningActive = false;
	@Getter
	private String targetName = null;
	private Actor lastTarget = null;
	private boolean targetCached = false;
	private int targetId = -1;
	private String targetNameRaw = null;
	private String targetNameCached = null;
	private String targetNameNormalized = null;
	private String lastTaskName = null;
	private String lastTaskNameLower = null;
	private String lastIgnoredNpcs = null;
	private Set<String> ignoredNpcNames = Set.of();
	private List<Pattern> ignoredNpcPatterns = List.of();
	private int ticksSinceInteractionEnd = -1;
	public void reset() {
		resetAlert();
		lastTarget = null;
		clearTargetCache();
		ticksSinceInteractionEnd = -1;
	}
	private void resetAlert() {
		showAlert = false;
		tooltipReason = null;
		conflictingHeadgearId = -1;
		isStackingWarningActive = false;
		targetName = null;
	}
	public void ignoreCurrentTarget() {
		if (targetName == null || targetName.isEmpty()) return;
		String ignoredNpcs = config.ignoredNpcs();
		if (ignoredNpcs == null) ignoredNpcs = "";
		Set<String> entries = new LinkedHashSet<>();
		Set<String> names = new HashSet<>();
		for (String entry : splitConfigList(ignoredNpcs)) {
			String name = normalize(entry);
			if (name.isEmpty() || !names.add(name)) continue;
			entries.add(entry.trim());
		}
		if (names.add(normalize(targetName))) entries.add(targetName);
		config.setIgnoredNpcs(Text.toCSV(entries));
		lastIgnoredNpcs = null;
		updateIgnoredNpcs();
		resetAlert();
	}
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() != GameState.LOGGED_IN) reset();
	}
	public void onInteractingChanged(InteractingChanged event) {
		if (event.getSource() != client.getLocalPlayer()) return;
		Actor target = event.getTarget();
		if (target != null) {
			lastTarget = target;
			clearTargetCache();
			ticksSinceInteractionEnd = -1;
		} else if (lastTarget != null) ticksSinceInteractionEnd = 0;
	}
	public void onGameTick() {
		if (ticksSinceInteractionEnd != -1) ticksSinceInteractionEnd++;
		updateAlertState();
		updateFlash();
	}
	private void updateAlertState() {
		if (lastTarget == null || (ticksSinceInteractionEnd != -1 && ticksSinceInteractionEnd > config.hideAlertDelay())) {
			reset();
			return;
		}
		if (!(lastTarget instanceof NPC)) {
			resetAlert();
			return;
		}
		isStackingWarningActive = false;
		NPC npc = (NPC) lastTarget;
		int npcId = npc.getId();
		if (npcId == -1) {
			resetAlert();
			return;
		}
		updateTargetName(npc, npcId);
		if (isIgnoredNpc()) {
			resetAlert();
			return;
		}
		boolean isUndead = SalveData.isUndeadNpc(npcId);
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		boolean wearingSalve = isWearingSalveAmulet(equipment);
		boolean isRelevantSlayerTask = false;
		if (config.showStackingWarning() && wearingSalve && !isUndead) {
			isRelevantSlayerTask = isSlayerTaskReminderActive();
		}
		if (config.showStackingWarning() && wearingSalve && (isUndead || isRelevantSlayerTask)) {
			int wornSlayerHelmId = getWornSlayerHelmOrBlackMaskId(equipment);
			if (wornSlayerHelmId != -1) {
				showAlert = true;
				tooltipReason = "Salve amulet and Black mask/Slayer helmet effects do not stack.";
				conflictingHeadgearId = wornSlayerHelmId;
				isStackingWarningActive = true;
				return;
			}
		}
		if (config.warnOnUselessSalve() && wearingSalve && !isUndead) {
			showAlert = true;
			tooltipReason = "Salve amulet is ineffective against non-undead monsters.";
			return;
		}
		if (!wearingSalve && (!isUndead || !config.alertOnUndeadCombat())) {
			isRelevantSlayerTask = isSlayerTaskReminderActive();
		}
		if (!wearingSalve && (isRelevantSlayerTask || (config.alertOnUndeadCombat() && isUndead))) {
			showAlert = true;
			if (isRelevantSlayerTask && !isUndead) tooltipReason = "On a slayer task where Salve amulet is effective.";
			else tooltipReason = "Attacking an undead monster without Salve amulet.";
			return;
		}
		resetAlert();
	}
	private void updateFlash() {
		if (!showAlert) {
			flash = false;
			return;
		}
		flash = !flash;
	}
	private int getWornSlayerHelmOrBlackMaskId(ItemContainer equipment) {
		if (equipment == null) return -1;
		Item helmet = equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		if (helmet == null) return -1;
		int helmetId = helmet.getId();
		if (SalveData.isBlackMask(helmetId)) return helmetId;
		return -1;
	}
	private boolean isSlayerTaskReminderActive() {
		if (!config.slayerTaskReminderEnabled()) return false;
		String taskName = configManager.getRSProfileConfiguration(SLAYER_PLUGIN_GROUP, SLAYER_TASK_NAME_KEY);
		if (taskName == null || taskName.isEmpty()) return false;
		if (!taskName.equals(lastTaskName)) {
			lastTaskName = taskName;
			lastTaskNameLower = taskName.toLowerCase(Locale.ROOT);
		}
		taskName = lastTaskNameLower;
		if (SalveData.MANDATORY_SLAYER_TASKS.contains(taskName)) return true;
		if (config.remindOnBlueDragonsTask() && SalveData.BLUE_DRAGON_TASK.equals(taskName)) return true;
		if (config.remindOnSkeletonsTask() && SalveData.SKELETON_TASK.equals(taskName)) return true;
		return config.remindOnOgresTask() && SalveData.OGRE_TASK.equals(taskName);
	}
	private boolean isWearingSalveAmulet(ItemContainer equipment) {
		if (equipment == null) return false;
		Item amulet = equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
		return amulet != null && SalveData.isSalveAmulet(amulet.getId());
	}
	private boolean isIgnoredNpc() {
		updateIgnoredNpcs();
		if (targetNameNormalized == null) return false;
		if (ignoredNpcNames.contains(targetNameNormalized)) return true;
		for (Pattern pattern : ignoredNpcPatterns) if (pattern.matcher(targetNameNormalized).matches()) return true;
		return false;
	}
	private void updateTargetName(NPC npc, int npcId) {
		String nameRaw = getNpcNameRaw(npc);
		if (!targetCached || npcId != targetId || !matchesTargetNameRaw(nameRaw)) {
			targetId = npcId;
			targetCached = true;
			targetNameRaw = nameRaw;
			targetNameCached = nameRaw == null ? null : Text.removeTags(nameRaw);
			targetName = targetNameCached;
			targetNameNormalized = targetName == null ? null : normalize(targetName);
			return;
		}
		targetName = targetNameCached;
	}
	private boolean matchesTargetNameRaw(String nameRaw) {
		if (targetNameRaw == null) return nameRaw == null;
		return targetNameRaw.equals(nameRaw);
	}
	private void clearTargetCache() {
		targetCached = false;
		targetId = -1;
		targetNameRaw = null;
		targetNameCached = null;
		targetNameNormalized = null;
	}
	private void updateIgnoredNpcs() {
		String ignoredNpcs = config.ignoredNpcs();
		if (ignoredNpcs == null) ignoredNpcs = "";
		if (ignoredNpcs.equals(lastIgnoredNpcs)) return;
		Set<String> names = new HashSet<>();
		List<Pattern> patterns = new ArrayList<>();
		for (String entry : splitConfigList(ignoredNpcs)) {
			String name = normalize(entry);
			if (name.isEmpty()) continue;
			if (name.indexOf('*') != -1 || name.indexOf('?') != -1) patterns.add(toPattern(name));
			else names.add(name);
		}
		lastIgnoredNpcs = ignoredNpcs;
		ignoredNpcNames = names;
		ignoredNpcPatterns = patterns;
	}
	private static List<String> splitConfigList(String config) {
		return Text.fromCSV(config.replace('\r', ',').replace('\n', ','));
	}
	private static String normalize(String text) {
		return Text.removeTags(text).replace('\u00A0', ' ').trim().toLowerCase(Locale.ROOT);
	}
	private static Pattern toPattern(String wildcard) {
		StringBuilder regex = new StringBuilder(wildcard.length() * 2 + 2);
		regex.append('^');
		for (int i = 0; i < wildcard.length(); i++) {
			char c = wildcard.charAt(i);
			if (c == '*') regex.append(".*");
			else if (c == '?') regex.append('.');
			else {
				if ("\\.[]{}()+-^$|".indexOf(c) != -1) regex.append('\\');
				regex.append(c);
			}
		}
		regex.append('$');
		return Pattern.compile(regex.toString());
	}
	private static String getNpcNameRaw(NPC npc) {
		NPCComposition composition = npc.getTransformedComposition();
		if (composition != null && composition.getName() != null) return composition.getName();
		return npc.getName();
	}
}
