package com.salvereminder.core;
import com.salvereminder.SalveReminderConfig;
import com.salvereminder.data.SalveData;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
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
	private static final String CONFIG_GROUP = "salvereminder";
	private static final String MIGRATED_KEY = "migrated";
	private static final String MIGRATED_VERSION = "2";
	private static final String OLD_SLAYER_TASK_ENABLED_KEY = "slayerTaskReminderEnabled";
	private static final String SLAYER_PLUGIN_GROUP = "slayer";
	private static final String SLAYER_TASK_NAME_KEY = "taskName";
	private static final int NO_ICON = -1;
	private enum AlertType {
		NONE,
		USELESS_TARGET,
		UNDEAD_TARGET,
		USELESS_TASK,
		SALVE_TASK,
		STACKING,
		TASK_OPTION
	}
	@Inject
	private Client client;
	@Inject
	private SalveReminderConfig config;
	@Inject
	private ConfigManager configManager;
	private boolean showAlert = false;
	@Getter
	private boolean flash = false;
	private String tooltipReason = null;
	private String targetName = null;
	private AlertType alertType = AlertType.NONE;
	private int alertItemId = NO_ICON;
	private int alertSpriteId = NO_ICON;
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
	private int transientTicks = -1;
	private String transientTooltipReason = null;
	private int transientItemId = NO_ICON;
	private int transientSpriteId = NO_ICON;
	private AlertType transientAlertType = AlertType.NONE;
	private boolean equipmentStateKnown = false;
	private boolean lastWearingSalve = false;
	private boolean lastWearingSlayerHeadgear = false;
	public void migrateConfig() {
		if (MIGRATED_VERSION.equals(configManager.getConfiguration(CONFIG_GROUP, MIGRATED_KEY))) return;
		configManager.unsetConfiguration(CONFIG_GROUP, OLD_SLAYER_TASK_ENABLED_KEY);
		configManager.setConfiguration(CONFIG_GROUP, MIGRATED_KEY, MIGRATED_VERSION);
	}
	public void reset() {
		resetAlert();
		resetTransientAlert();
		resetTarget();
		equipmentStateKnown = false;
	}
	public boolean isShowAlert() {
		return getDebugAlert() != SalveReminderConfig.DebugAlert.OFF || showAlert;
	}
	public String getTooltipReason() {
		SalveReminderConfig.DebugAlert debugAlert = getDebugAlert();
		if (debugAlert != SalveReminderConfig.DebugAlert.OFF) return getDebugTooltipReason(debugAlert);
		return tooltipReason;
	}
	public int getAlertItemId() {
		SalveReminderConfig.DebugAlert debugAlert = getDebugAlert();
		if (debugAlert != SalveReminderConfig.DebugAlert.OFF) return getDebugItemId(debugAlert);
		if (alertType == AlertType.STACKING) return getStackingAlertItemId(alertItemId);
		return alertItemId;
	}
	public int getAlertSpriteId() {
		SalveReminderConfig.DebugAlert debugAlert = getDebugAlert();
		if (debugAlert != SalveReminderConfig.DebugAlert.OFF) return getDebugSpriteId(debugAlert);
		if (alertType == AlertType.STACKING) return NO_ICON;
		return alertSpriteId;
	}
	public boolean isAlertIconCrossed() {
		SalveReminderConfig.DebugAlert debugAlert = getDebugAlert();
		if (debugAlert == SalveReminderConfig.DebugAlert.USELESS_TARGET || debugAlert == SalveReminderConfig.DebugAlert.USELESS_TASK) return true;
		return debugAlert == SalveReminderConfig.DebugAlert.OFF && (alertType == AlertType.USELESS_TARGET || alertType == AlertType.USELESS_TASK);
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
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (!isGameObjectAction(event.getMenuAction())) return;
		if (!"Attack".equalsIgnoreCase(event.getMenuOption())) return;
		if (!isUndeadObject(event.getId())) return;
		triggerUndeadObjectAlert();
		updateAlertState();
	}
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() != InventoryID.WORN) return;
		ItemContainer equipment = event.getItemContainer();
		boolean wearingSalve = isWearingSalveAmulet(equipment);
		int wornHeadgearId = getWornSlayerHelmOrBlackMaskId(equipment);
		boolean wearingHeadgear = wornHeadgearId != NO_ICON;
		boolean becameStacked = wearingSalve && wearingHeadgear && (!equipmentStateKnown || !lastWearingSalve || !lastWearingSlayerHeadgear);
		equipmentStateKnown = true;
		lastWearingSalve = wearingSalve;
		lastWearingSlayerHeadgear = wearingHeadgear;
		if (becameStacked && config.showStackingWarning()) setTransientStackingAlert(wornHeadgearId);
		else if ((!wearingSalve || !wearingHeadgear) && transientAlertType == AlertType.STACKING) resetTransientAlert();
		updateAlertState();
	}
	public void onConfigChanged(ConfigChanged event) {
		if (!SLAYER_PLUGIN_GROUP.equals(event.getGroup()) || !SLAYER_TASK_NAME_KEY.equals(event.getKey())) return;
		lastTaskName = null;
		lastTaskNameLower = null;
		if (event.getNewValue() == null || event.getNewValue().isEmpty()) {
			if (isTransientTaskAlert()) resetTransientAlert();
			updateAlertState();
			return;
		}
		triggerTaskAlert();
		updateAlertState();
	}
	public void onGameTick() {
		if (ticksSinceInteractionEnd != -1) ticksSinceInteractionEnd++;
		if (transientTicks != -1) transientTicks++;
		updateAlertState();
		updateFlash();
	}
	private void updateAlertState() {
		if (lastTarget == null || (ticksSinceInteractionEnd != -1 && ticksSinceInteractionEnd > config.hideAlertDelay())) {
			resetTarget();
			applyTransientAlert();
			return;
		}
		if (!(lastTarget instanceof NPC)) {
			resetAlert();
			applyTransientAlert();
			return;
		}
		NPC npc = (NPC) lastTarget;
		int npcId = npc.getId();
		if (npcId == -1) {
			resetAlert();
			applyTransientAlert();
			return;
		}
		updateTargetName(npc, npcId);
		if (isIgnoredNpc()) {
			resetAlert();
			applyTransientAlert();
			return;
		}
		boolean isUndead = SalveData.isUndeadNpc(npcId);
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		boolean wearingSalve = isWearingSalveAmulet(equipment);
		int wornHeadgearId = getWornSlayerHelmOrBlackMaskId(equipment);
		if (config.showStackingWarning() && wearingSalve && wornHeadgearId != NO_ICON) {
			setStackingAlert(wornHeadgearId);
			return;
		}
		String taskName = getCurrentTaskName();
		if (taskName != null && isOptionalTaskReminderEnabled(taskName) && isTaskBaseTarget(taskName) && (!isUndead || wearingSalve)) {
			setTaskOptionAlert(taskName);
			return;
		}
		if (config.warnOnUselessSalve() && wearingSalve && !isUndead) {
			setItemAlert(AlertType.USELESS_TARGET, "Salve amulet is ineffective against the current target.", config.displayIcon().getItemID());
			return;
		}
		if (config.alertOnUndeadCombat() && !wearingSalve && isUndead) {
			setItemAlert(AlertType.UNDEAD_TARGET, "Attacking an undead enemy without Salve amulet.", config.displayIcon().getItemID());
			return;
		}
		applyTransientAlert();
	}
	private void triggerTaskAlert() {
		String taskName = getCurrentTaskName();
		if (taskName == null) return;
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		boolean wearingSalve = isWearingSalveAmulet(equipment);
		int wornHeadgearId = getWornSlayerHelmOrBlackMaskId(equipment);
		if (config.showStackingWarning() && wearingSalve && wornHeadgearId != NO_ICON) {
			setTransientStackingAlert(wornHeadgearId);
			return;
		}
		if (isOptionalTaskReminderEnabled(taskName)) {
			setTransientTaskOptionAlert(taskName);
			return;
		}
		boolean salveTask = isSalveTask(taskName);
		if (config.warnOnUselessSalve() && wearingSalve && !salveTask) {
			setTransientItemAlert(AlertType.USELESS_TASK, "Salve amulet is not effective for current slayer task.", config.displayIcon().getItemID());
			return;
		}
		if (config.alertOnUndeadCombat() && !wearingSalve && salveTask) {
			setTransientTaskSalveAlert();
			return;
		}
		if (isTransientTaskAlert()) resetTransientAlert();
	}
	private void triggerUndeadObjectAlert() {
		resetTarget();
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		boolean wearingSalve = isWearingSalveAmulet(equipment);
		int wornHeadgearId = getWornSlayerHelmOrBlackMaskId(equipment);
		if (config.showStackingWarning() && wearingSalve && wornHeadgearId != NO_ICON) {
			setTransientStackingAlert(wornHeadgearId);
			return;
		}
		if (config.alertOnUndeadCombat() && !wearingSalve) {
			setTransientItemAlert(AlertType.UNDEAD_TARGET, "Attacking an undead enemy without Salve amulet.", config.displayIcon().getItemID());
			return;
		}
		if (transientAlertType == AlertType.UNDEAD_TARGET) resetTransientAlert();
	}
	private void setStackingAlert(int wornHeadgearId) {
		setItemAlert(AlertType.STACKING, "Salve amulet does not stack with Black mask or Slayer helmet.", wornHeadgearId);
	}
	private void setTransientStackingAlert(int wornHeadgearId) {
		setTransientItemAlert(AlertType.STACKING, "Salve amulet does not stack with Black mask or Slayer helmet.", wornHeadgearId);
	}
	private void setTaskOptionAlert(String taskName) {
		String option = SalveData.getTaskOptionName(taskName);
		if (option == null) option = "Salve";
		setTaskIconAlert(option + " is a Salve option for current slayer task.", taskName);
	}
	private void setTransientTaskOptionAlert(String taskName) {
		String option = SalveData.getTaskOptionName(taskName);
		if (option == null) option = "Salve";
		setTransientTaskIconAlert(option + " is a Salve option for current slayer task.", taskName);
	}
	private void setTransientTaskSalveAlert() {
		setTransientItemAlert(AlertType.SALVE_TASK, "Salve amulet is effective for current slayer task.", config.displayIcon().getItemID());
	}
	private void setTaskIconAlert(String tooltip, String taskName) {
		int itemId = SalveData.getTaskItemId(taskName);
		if (itemId == NO_ICON) itemId = config.displayIcon().getItemID();
		setAlert(AlertType.TASK_OPTION, tooltip, itemId, SalveData.getTaskSpriteId(taskName));
	}
	private void setTransientTaskIconAlert(String tooltip, String taskName) {
		int itemId = SalveData.getTaskItemId(taskName);
		if (itemId == NO_ICON) itemId = config.displayIcon().getItemID();
		setTransientAlert(AlertType.TASK_OPTION, tooltip, itemId, SalveData.getTaskSpriteId(taskName));
	}
	private void setItemAlert(AlertType type, String tooltip, int itemId) {
		setAlert(type, tooltip, itemId, NO_ICON);
	}
	private void setTransientItemAlert(AlertType type, String tooltip, int itemId) {
		setTransientAlert(type, tooltip, itemId, NO_ICON);
	}
	private void setAlert(AlertType type, String tooltip, int itemId, int spriteId) {
		showAlert = true;
		alertType = type;
		tooltipReason = tooltip;
		alertItemId = itemId;
		alertSpriteId = spriteId;
	}
	private void setTransientAlert(AlertType type, String tooltip, int itemId, int spriteId) {
		transientAlertType = type;
		transientTooltipReason = tooltip;
		transientItemId = itemId;
		transientSpriteId = spriteId;
		transientTicks = 0;
	}
	private void applyTransientAlert() {
		if (transientAlertType == AlertType.NONE) {
			resetAlert();
			return;
		}
		if (transientTicks > config.hideAlertDelay()) {
			resetTransientAlert();
			resetAlert();
			return;
		}
		setAlert(transientAlertType, transientTooltipReason, transientItemId, transientSpriteId);
	}
	private void updateFlash() {
		if (isShowAlert()) {
			flash = !flash;
			return;
		}
		flash = false;
	}
	private int getWornSlayerHelmOrBlackMaskId(ItemContainer equipment) {
		if (equipment == null) return NO_ICON;
		Item helmet = equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		if (helmet == null) return NO_ICON;
		int helmetId = helmet.getId();
		if (SalveData.isBlackMask(helmetId)) return helmetId;
		return NO_ICON;
	}
	private static boolean isGameObjectAction(MenuAction action) {
		switch (action) {
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
				return true;
			default:
				return false;
		}
	}
	private int getStackingAlertItemId(int fallbackItemId) {
		if (!flash) return config.displayIcon().getItemID();
		SalveReminderConfig.StackingIcon icon = config.stackingIcon();
		if (icon != null && icon.getItemID() != NO_ICON) return icon.getItemID();
		if (fallbackItemId != NO_ICON) return fallbackItemId;
		return ItemID.SLAYER_HELM;
	}
	private boolean isSalveTask(String taskName) {
		return SalveData.MANDATORY_SLAYER_TASKS.contains(taskName) || isOptionalTaskReminderEnabled(taskName);
	}
	private boolean isOptionalTaskReminderEnabled(String taskName) {
		if (config.remindOnBlueDragonsTask() && SalveData.BLUE_DRAGON_TASK.equals(taskName)) return true;
		if (config.remindOnSkeletonsTask() && SalveData.SKELETON_TASK.equals(taskName)) return true;
		return config.remindOnOgresTask() && SalveData.OGRE_TASK.equals(taskName);
	}
	private boolean isTaskBaseTarget(String taskName) {
		if (targetNameNormalized == null) return false;
		if (SalveData.BLUE_DRAGON_TASK.equals(taskName)) return targetNameNormalized.endsWith("blue dragon");
		if (SalveData.SKELETON_TASK.equals(taskName)) return targetNameNormalized.contains("skeleton");
		if (!SalveData.OGRE_TASK.equals(taskName)) return false;
		if (!targetNameNormalized.contains("ogre")) return false;
		return !targetNameNormalized.contains("zogre") && !targetNameNormalized.contains("skogre");
	}
	private String getCurrentTaskName() {
		String taskName = configManager.getRSProfileConfiguration(SLAYER_PLUGIN_GROUP, SLAYER_TASK_NAME_KEY);
		if (taskName == null) return null;
		taskName = taskName.trim();
		if (taskName.isEmpty()) return null;
		if (!taskName.equals(lastTaskName)) {
			lastTaskName = taskName;
			lastTaskNameLower = taskName.toLowerCase(Locale.ROOT);
		}
		return lastTaskNameLower;
	}
	private boolean isWearingSalveAmulet(ItemContainer equipment) {
		if (equipment == null) return false;
		Item amulet = equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
		return amulet != null && SalveData.isSalveAmulet(amulet.getId());
	}
	private boolean isUndeadObject(int id) {
		if (id < 0) return false;
		if (SalveData.isUndeadObject(id)) return true;
		ObjectComposition composition = client.getObjectDefinition(id);
		if (composition == null || composition.getImpostorIds() == null) return false;
		ObjectComposition impostor = composition.getImpostor();
		return impostor != null && SalveData.isUndeadObject(impostor.getId());
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
	private void resetAlert() {
		showAlert = false;
		tooltipReason = null;
		targetName = null;
		alertItemId = NO_ICON;
		alertSpriteId = NO_ICON;
		alertType = AlertType.NONE;
	}
	private void resetTransientAlert() {
		transientTicks = -1;
		transientTooltipReason = null;
		transientItemId = NO_ICON;
		transientSpriteId = NO_ICON;
		transientAlertType = AlertType.NONE;
	}
	private void resetTarget() {
		lastTarget = null;
		clearTargetCache();
		ticksSinceInteractionEnd = -1;
	}
	private boolean isTransientTaskAlert() {
		return transientAlertType == AlertType.USELESS_TASK || transientAlertType == AlertType.SALVE_TASK || transientAlertType == AlertType.TASK_OPTION;
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
	private SalveReminderConfig.DebugAlert getDebugAlert() {
		SalveReminderConfig.DebugAlert debugAlert = config.debugAlert();
		if (debugAlert == null) return SalveReminderConfig.DebugAlert.OFF;
		return debugAlert;
	}
	private String getDebugTooltipReason(SalveReminderConfig.DebugAlert debugAlert) {
		switch (debugAlert) {
			case USELESS_TARGET:
				return "Salve amulet is ineffective against the current target.";
			case UNDEAD_TARGET:
				return "Attacking an undead enemy without Salve amulet.";
			case USELESS_TASK:
				return "Salve amulet is not effective for current slayer task.";
			case SALVE_TASK:
				return "Salve amulet is effective for current slayer task.";
			case STACKING:
				return "Salve amulet does not stack with Black mask or Slayer helmet.";
			case VORKATH_TASK:
				return "Vorkath is a Salve option for current slayer task.";
			case VETION_TASK:
				return "Vet'ion/Calvar'ion are Salve options for current slayer task.";
			case ZOGRE_TASK:
				return "Zogres/Skogres are Salve options for current slayer task.";
			default:
				return null;
		}
	}
	private int getDebugItemId(SalveReminderConfig.DebugAlert debugAlert) {
		switch (debugAlert) {
			case STACKING:
				return getStackingAlertItemId(ItemID.SLAYER_HELM);
			case VORKATH_TASK:
				return SalveData.getTaskItemId(SalveData.BLUE_DRAGON_TASK);
			case VETION_TASK:
				return SalveData.getTaskItemId(SalveData.SKELETON_TASK);
			case ZOGRE_TASK:
				return SalveData.getTaskItemId(SalveData.OGRE_TASK);
			default:
				return config.displayIcon().getItemID();
		}
	}
	private int getDebugSpriteId(SalveReminderConfig.DebugAlert debugAlert) {
		switch (debugAlert) {
			case VORKATH_TASK:
				return SalveData.getTaskSpriteId(SalveData.BLUE_DRAGON_TASK);
			case VETION_TASK:
				return SalveData.getTaskSpriteId(SalveData.SKELETON_TASK);
			default:
				return NO_ICON;
		}
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
