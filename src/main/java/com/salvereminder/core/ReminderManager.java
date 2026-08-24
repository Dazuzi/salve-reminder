package com.salvereminder.core;
import com.salvereminder.SalveReminderConfig;
import com.salvereminder.data.SalveData;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
@Singleton
public class ReminderManager {
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
	private static final class FeatureSettings {
		private static final FeatureSettings DISABLED = new FeatureSettings(false, false, false, false, false, false);
		private final boolean alertOnUndead;
		private final boolean stackingWarning;
		private final boolean warnOnUseless;
		private final boolean blueDragonTasks;
		private final boolean skeletonTasks;
		private final boolean ogreTasks;
		private final boolean optionalTasks;
		private final boolean combatEffects;
		private final boolean targetEffects;
		private final boolean objectAlerts;
		private final boolean enabled;
		private FeatureSettings(boolean alertOnUndead, boolean stackingWarning, boolean warnOnUseless, boolean blueDragonTasks, boolean skeletonTasks, boolean ogreTasks) {
			this.alertOnUndead = alertOnUndead;
			this.stackingWarning = stackingWarning;
			this.warnOnUseless = warnOnUseless;
			this.blueDragonTasks = blueDragonTasks;
			this.skeletonTasks = skeletonTasks;
			this.ogreTasks = ogreTasks;
			optionalTasks = blueDragonTasks || skeletonTasks || ogreTasks;
			combatEffects = alertOnUndead || warnOnUseless;
			targetEffects = combatEffects || optionalTasks;
			objectAlerts = alertOnUndead || stackingWarning;
			enabled = targetEffects || stackingWarning;
		}
	}
	private final Client client;
	private final SalveReminderConfig config;
	private final ConfigManager configManager;
	private volatile FeatureSettings features = FeatureSettings.DISABLED;
	private volatile SalveReminderConfig.DebugAlert debugAlert = SalveReminderConfig.DebugAlert.OFF;
	private int hideAlertDelay = 0;
	private volatile SalveReminderConfig.SalveIcon displayIcon = null;
	private volatile SalveReminderConfig.StackingIcon stackingIcon = null;
	private volatile boolean flash = false;
	private volatile String tooltipReason = null;
	private volatile String targetName = null;
	private volatile AlertType alertType = AlertType.NONE;
	private int alertItemId = NO_ICON;
	private int alertSpriteId = NO_ICON;
	private volatile Actor lastTarget = null;
	private boolean targetDirty = false;
	private boolean captureTargetOnTick = false;
	private int targetId = -1;
	private String targetNameCached = null;
	private String targetNameNormalized = null;
	private boolean taskNameKnown = false;
	private String currentTaskName = null;
	private String lastIgnoredNpcs = null;
	private Set<String> ignoredNpcNames = Set.of();
	private Set<String> ignoredNpcWildcards = Set.of();
	private int ticksSinceInteractionEnd = -1;
	private int transientTicks = 0;
	private String transientTooltipReason = null;
	private int transientItemId = NO_ICON;
	private int transientSpriteId = NO_ICON;
	private AlertType transientAlertType = AlertType.NONE;
	private boolean equipmentStateKnown = false;
	private boolean lastWearingSalve = false;
	private int lastWornHeadgearId = NO_ICON;
	@Inject
	public ReminderManager(Client client, SalveReminderConfig config, ConfigManager configManager) {
		this.client = client;
		this.config = config;
		this.configManager = configManager;
	}
	public synchronized void start() {
		reset();
		features = getConfiguredFeatures();
		debugAlert = getConfiguredDebugAlert();
		hideAlertDelay = features.enabled ? config.hideAlertDelay() : 0;
		captureTargetOnTick = features.enabled;
	}
	public synchronized void reset() {
		resetAlert();
		resetTransientAlert();
		resetTarget();
		flash = false;
		targetDirty = false;
		captureTargetOnTick = false;
		taskNameKnown = false;
		currentTaskName = null;
		lastIgnoredNpcs = null;
		ignoredNpcNames = Set.of();
		ignoredNpcWildcards = Set.of();
		equipmentStateKnown = false;
		lastWearingSalve = false;
		lastWornHeadgearId = NO_ICON;
		displayIcon = null;
		stackingIcon = null;
	}
	public boolean isShowAlert() {
		return debugAlert != SalveReminderConfig.DebugAlert.OFF || alertType != AlertType.NONE;
	}
	public boolean hasEnabledAlerts() {
		return features.enabled || debugAlert != SalveReminderConfig.DebugAlert.OFF;
	}
	public boolean isTrackingDisabled() {
		return !features.enabled;
	}
	public boolean isFlash() {
		return flash;
	}
	public String getTooltipReason() {
		SalveReminderConfig.DebugAlert debug = debugAlert;
		if (debug != SalveReminderConfig.DebugAlert.OFF) return getDebugTooltipReason(debug);
		return tooltipReason;
	}
	public int getAlertItemId() {
		SalveReminderConfig.DebugAlert debug = debugAlert;
		if (debug != SalveReminderConfig.DebugAlert.OFF) return getDebugItemId(debug);
		if (alertType == AlertType.STACKING) return getStackingAlertItemId(alertItemId);
		return alertItemId;
	}
	public int getAlertSpriteId() {
		SalveReminderConfig.DebugAlert debug = debugAlert;
		if (debug != SalveReminderConfig.DebugAlert.OFF) return getDebugSpriteId(debug);
		if (alertType == AlertType.STACKING) return NO_ICON;
		return alertSpriteId;
	}
	public boolean isAlertIconCrossed() {
		SalveReminderConfig.DebugAlert debug = debugAlert;
		if (debug == SalveReminderConfig.DebugAlert.USELESS_TARGET || debug == SalveReminderConfig.DebugAlert.USELESS_TASK) return true;
		AlertType type = alertType;
		return debug == SalveReminderConfig.DebugAlert.OFF && (type == AlertType.USELESS_TARGET || type == AlertType.USELESS_TASK);
	}
	public void ignoreCurrentTarget() {
		String currentTargetName = targetName;
		if (currentTargetName == null || currentTargetName.isEmpty()) return;
		String ignoredNpcs = config.ignoredNpcs();
		if (ignoredNpcs == null) ignoredNpcs = "";
		Map<String, String> entries = new LinkedHashMap<>();
		for (String entry : splitConfigList(ignoredNpcs)) {
			String name = normalize(entry);
			if (!name.isEmpty()) entries.putIfAbsent(name, entry.trim());
		}
		entries.putIfAbsent(normalizePlain(currentTargetName), currentTargetName);
		String updatedIgnoredNpcs = Text.toCSV(entries.values());
		config.setIgnoredNpcs(updatedIgnoredNpcs);
		synchronized (this) {
			updateIgnoredNpcs(updatedIgnoredNpcs);
			resetAlert();
			targetDirty = features.enabled;
		}
	}
	public synchronized void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN || !features.enabled) return;
		reset();
		captureTargetOnTick = features.enabled;
	}
	public void onInteractingChanged(InteractingChanged event) {
		if (!features.enabled) return;
		Actor source = event.getSource();
		if (!(source instanceof Player) || source != client.getLocalPlayer()) return;
		synchronized (this) {
			if (!features.enabled) return;
			Actor target = event.getTarget();
			if (target != null) {
				if (target == lastTarget) {
					ticksSinceInteractionEnd = -1;
					return;
				}
				lastTarget = target;
				clearTargetCache();
				ticksSinceInteractionEnd = -1;
			} else if (lastTarget != null) ticksSinceInteractionEnd = 0;
			else return;
			targetDirty = true;
		}
	}
	public void onNpcChanged(NpcChanged event) {
		if (!features.enabled || event.getNpc() != lastTarget) return;
		synchronized (this) {
			if (!features.enabled || event.getNpc() != lastTarget) return;
			clearTargetCache();
			targetDirty = true;
		}
	}
	public void onNpcDespawned(NpcDespawned event) {
		if (!features.enabled || event.getNpc() != lastTarget) return;
		synchronized (this) {
			if (!features.enabled || event.getNpc() != lastTarget || ticksSinceInteractionEnd != -1) return;
			ticksSinceInteractionEnd = 0;
		}
	}
	public boolean onMenuOptionClicked(MenuOptionClicked event) {
		if (!features.objectAlerts) return false;
		if (!isGameObjectAction(event.getMenuAction())) return false;
		if (!"Attack".equalsIgnoreCase(event.getMenuOption())) return false;
		if (!isUndeadObject(event.getId())) return false;
		synchronized (this) {
			if (!features.objectAlerts) return false;
			boolean wasShowing = isShowAlert();
			triggerUndeadObjectAlert();
			refreshAlertState();
			return wasShowing != isShowAlert();
		}
	}
	public boolean onItemContainerChanged(ItemContainerChanged event) {
		if (!features.enabled || event.getContainerId() != InventoryID.WORN) return false;
		synchronized (this) {
			if (!features.enabled) return false;
			boolean wasShowing = isShowAlert();
			boolean known = equipmentStateKnown;
			boolean wasWearingSalve = lastWearingSalve;
			int wasWornHeadgearId = lastWornHeadgearId;
			updateEquipmentState(event.getItemContainer());
			if (known && wasWearingSalve == lastWearingSalve && wasWornHeadgearId == lastWornHeadgearId) return false;
			boolean becameStacked = lastWearingSalve && lastWornHeadgearId != NO_ICON && (!known || !wasWearingSalve || wasWornHeadgearId == NO_ICON);
			if (becameStacked && features.stackingWarning) setStackingAlert(true, lastWornHeadgearId);
			else if ((!lastWearingSalve || lastWornHeadgearId == NO_ICON) && transientAlertType == AlertType.STACKING) resetTransientAlert();
			refreshAlertState();
			return wasShowing != isShowAlert();
		}
	}
	public synchronized void onConfigChanged(ConfigChanged event) {
		if (SalveReminderConfig.GROUP.equals(event.getGroup())) {
			onOwnConfigChanged(event);
			return;
		}
		if (!features.enabled || !SLAYER_PLUGIN_GROUP.equals(event.getGroup()) || !SLAYER_TASK_NAME_KEY.equals(event.getKey())) return;
		setCurrentTaskName(event.getNewValue());
		if (currentTaskName == null) {
			if (isTransientTaskAlert()) resetTransientAlert();
			refreshAlertState();
			return;
		}
		triggerTaskAlert();
		refreshAlertState();
	}
	public synchronized void onProfileChanged() {
		reset();
		features = getConfiguredFeatures();
		debugAlert = getConfiguredDebugAlert();
		hideAlertDelay = features.enabled ? config.hideAlertDelay() : 0;
		captureTargetOnTick = features.enabled;
	}
	public synchronized void onRuneScapeProfileChanged() {
		if (!features.enabled) return;
		reset();
		captureTargetOnTick = features.enabled;
	}
	public synchronized boolean onGameTick() {
		if (!features.enabled && debugAlert == SalveReminderConfig.DebugAlert.OFF) return false;
		boolean wasShowing = isShowAlert();
		boolean refresh = targetDirty;
		if (features.enabled && captureTargetOnTick) {
			captureCurrentTarget();
			refresh = true;
		}
		if (ticksSinceInteractionEnd != -1 || transientAlertType != AlertType.NONE) {
			int hideDelay = hideAlertDelay;
			if (ticksSinceInteractionEnd != -1 && ++ticksSinceInteractionEnd > hideDelay) {
				resetTarget();
				refresh = true;
			}
			if (transientAlertType != AlertType.NONE && ++transientTicks > hideDelay) {
				resetTransientAlert();
				refresh = true;
			}
		}
		if (refresh) refreshAlertState();
		boolean showing = isShowAlert();
		updateFlash(showing);
		return wasShowing != showing;
	}
	private void onOwnConfigChanged(ConfigChanged event) {
		String key = event.getKey();
		if ("ignoredNpcs".equals(key)) {
			if (!features.enabled) return;
			updateIgnoredNpcs(event.getNewValue());
			if (isIgnoredNpc()) resetAlert();
			targetDirty = true;
			return;
		}
		if ("displayIcon".equals(key)) {
			displayIcon = null;
			targetDirty = features.enabled;
			return;
		}
		if ("stackingIcon".equals(key)) {
			stackingIcon = null;
			return;
		}
		if ("hideAlertDelay".equals(key)) {
			hideAlertDelay = features.enabled ? config.hideAlertDelay() : 0;
			return;
		}
		if ("debugAlert".equals(key)) {
			debugAlert = getConfiguredDebugAlert();
			if (!features.enabled && debugAlert == SalveReminderConfig.DebugAlert.OFF) flash = false;
			return;
		}
		if (!isFeatureConfig(key)) return;
		FeatureSettings previousFeatures = features;
		boolean wasTracking = previousFeatures.enabled;
		features = getConfiguredFeatures();
		if (!features.enabled) {
			if (wasTracking) reset();
			hideAlertDelay = 0;
			return;
		}
		if (!wasTracking) {
			reset();
			hideAlertDelay = config.hideAlertDelay();
			captureTargetOnTick = true;
			return;
		}
		if (!previousFeatures.stackingWarning && features.stackingWarning) equipmentStateKnown = false;
		targetDirty = true;
	}
	private FeatureSettings getConfiguredFeatures() {
		boolean alertOnUndead = config.alertOnUndeadCombat();
		boolean stackingWarning = config.showStackingWarning();
		boolean warnOnUseless = config.warnOnUselessSalve();
		boolean blueDragonTasks = config.remindOnBlueDragonsTask();
		boolean skeletonTasks = config.remindOnSkeletonsTask();
		boolean ogreTasks = config.remindOnOgresTask();
		if (!(alertOnUndead || stackingWarning || warnOnUseless || blueDragonTasks || skeletonTasks || ogreTasks)) return FeatureSettings.DISABLED;
		return new FeatureSettings(alertOnUndead, stackingWarning, warnOnUseless, blueDragonTasks, skeletonTasks, ogreTasks);
	}
	private static boolean isFeatureConfig(String key) {
		return "alertOnUndeadCombat".equals(key) || "showStackingWarning".equals(key) || "warnOnUselessSalve".equals(key) || "remindOnBlueDragonsTask".equals(key) || "remindOnSkeletonsTask".equals(key) || "remindOnOgresTask".equals(key);
	}
	private SalveReminderConfig.DebugAlert getConfiguredDebugAlert() {
		SalveReminderConfig.DebugAlert alert = config.debugAlert();
		return alert == null ? SalveReminderConfig.DebugAlert.OFF : alert;
	}
	private void captureCurrentTarget() {
		if (lastTarget != null) {
			captureTargetOnTick = false;
			return;
		}
		Player player = client.getLocalPlayer();
		if (player == null) return;
		captureTargetOnTick = false;
		Actor target = player.getInteracting();
		if (target == null) return;
		lastTarget = target;
		clearTargetCache();
		ticksSinceInteractionEnd = -1;
	}
	private void refreshAlertState() {
		targetDirty = false;
		updateAlertState();
	}
	private void updateAlertState() {
		if (lastTarget == null || (ticksSinceInteractionEnd != -1 && ticksSinceInteractionEnd > hideAlertDelay)) {
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
		if (setStackingAlertIfNeeded(false)) return;
		if (!features.targetEffects) {
			applyTransientAlert();
			return;
		}
		boolean isUndead = false;
		boolean undeadKnown = false;
		if (features.optionalTasks) {
			String taskName = getCurrentTaskName();
			if (taskName != null && isOptionalTaskReminderEnabled(taskName) && isTaskBaseTarget(taskName)) {
				isUndead = SalveData.isUndeadNpc(npcId);
				undeadKnown = true;
				if (!isUndead || lastWearingSalve) {
					setTaskOptionAlert(false, taskName);
					return;
				}
			}
		}
		if (!features.combatEffects) {
			applyTransientAlert();
			return;
		}
		if (!undeadKnown) isUndead = SalveData.isUndeadNpc(npcId);
		if (features.warnOnUseless && lastWearingSalve && !isUndead) {
			setItemAlert(false, AlertType.USELESS_TARGET, "Salve amulet is ineffective against the current target.", getDisplayIconId());
			return;
		}
		if (features.alertOnUndead && !lastWearingSalve && isUndead) {
			setItemAlert(false, AlertType.UNDEAD_TARGET, "Attacking an undead enemy without Salve amulet.", getDisplayIconId());
			return;
		}
		applyTransientAlert();
	}
	private void triggerTaskAlert() {
		String taskName = getCurrentTaskName();
		if (taskName == null) return;
		if (setStackingAlertIfNeeded(true)) return;
		if (!features.targetEffects) {
			if (isTransientTaskAlert()) resetTransientAlert();
			return;
		}
		boolean optionalTask = isOptionalTaskReminderEnabled(taskName);
		if (optionalTask) {
			setTaskOptionAlert(true, taskName);
			return;
		}
		if (!features.combatEffects) {
			if (isTransientTaskAlert()) resetTransientAlert();
			return;
		}
		boolean salveTask = SalveData.isMandatorySlayerTask(taskName);
		if (features.warnOnUseless && lastWearingSalve && !salveTask) {
			setItemAlert(true, AlertType.USELESS_TASK, "Salve amulet is not effective for current slayer task.", getDisplayIconId());
			return;
		}
		if (features.alertOnUndead && !lastWearingSalve && salveTask) {
			setItemAlert(true, AlertType.SALVE_TASK, "Salve amulet is effective for current slayer task.", getDisplayIconId());
			return;
		}
		if (isTransientTaskAlert()) resetTransientAlert();
	}
	private void triggerUndeadObjectAlert() {
		resetTarget();
		if (setStackingAlertIfNeeded(true)) return;
		if (features.alertOnUndead && !lastWearingSalve) {
			setItemAlert(true, AlertType.UNDEAD_TARGET, "Attacking an undead enemy without Salve amulet.", getDisplayIconId());
			return;
		}
		if (transientAlertType == AlertType.UNDEAD_TARGET) resetTransientAlert();
	}
	private boolean setStackingAlertIfNeeded(boolean transientAlert) {
		updateEquipmentState();
		if (!features.stackingWarning || !lastWearingSalve || lastWornHeadgearId == NO_ICON) return false;
		setStackingAlert(transientAlert, lastWornHeadgearId);
		return true;
	}
	private void setStackingAlert(boolean transientAlert, int wornHeadgearId) {
		setItemAlert(transientAlert, AlertType.STACKING, "Salve amulet does not stack with Black mask or Slayer helmet.", wornHeadgearId);
	}
	private void setTaskOptionAlert(boolean transientAlert, String taskName) {
		String option = SalveData.getTaskOptionName(taskName);
		if (option == null) option = "Salve";
		setTaskIconAlert(transientAlert, option + " is a Salve option for current slayer task.", taskName);
	}
	private void setTaskIconAlert(boolean transientAlert, String tooltip, String taskName) {
		int itemId = SalveData.getTaskItemId(taskName);
		if (itemId == NO_ICON) itemId = getDisplayIconId();
		setAlert(transientAlert, AlertType.TASK_OPTION, tooltip, itemId, SalveData.getTaskSpriteId(taskName));
	}
	private void setItemAlert(boolean transientAlert, AlertType type, String tooltip, int itemId) {
		setAlert(transientAlert, type, tooltip, itemId, NO_ICON);
	}
	private void setAlert(boolean transientAlert, AlertType type, String tooltip, int itemId, int spriteId) {
		if (transientAlert) {
			transientAlertType = type;
			transientTooltipReason = tooltip;
			transientItemId = itemId;
			transientSpriteId = spriteId;
			transientTicks = 0;
			return;
		}
		tooltipReason = tooltip;
		alertItemId = itemId;
		alertSpriteId = spriteId;
		alertType = type;
	}
	private void applyTransientAlert() {
		if (transientAlertType == AlertType.NONE) {
			resetAlert();
			return;
		}
		if (transientTicks > hideAlertDelay) {
			resetTransientAlert();
			resetAlert();
			return;
		}
		setAlert(false, transientAlertType, transientTooltipReason, transientItemId, transientSpriteId);
	}
	private void updateFlash(boolean showing) {
		if (showing && !flash) {
			flash = true;
			return;
		}
		if (flash) flash = false;
	}
	private void updateEquipmentState() {
		if (equipmentStateKnown) return;
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment != null) updateEquipmentState(equipment);
	}
	private void updateEquipmentState(ItemContainer equipment) {
		lastWearingSalve = isWearingSalveAmulet(equipment);
		lastWornHeadgearId = features.stackingWarning ? getWornSlayerHelmOrBlackMaskId(equipment) : NO_ICON;
		equipmentStateKnown = true;
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
		if (!flash) return getDisplayIconId();
		SalveReminderConfig.StackingIcon icon = getStackingIcon();
		if (icon != null && icon.getItemID() != NO_ICON) return icon.getItemID();
		if (fallbackItemId != NO_ICON) return fallbackItemId;
		return ItemID.SLAYER_HELM;
	}
	private int getDisplayIconId() {
		SalveReminderConfig.SalveIcon icon = displayIcon;
		if (icon == null) synchronized (this) {
			if ((icon = displayIcon) == null) displayIcon = icon = config.displayIcon();
		}
		return icon.getItemID();
	}
	private SalveReminderConfig.StackingIcon getStackingIcon() {
		SalveReminderConfig.StackingIcon icon = stackingIcon;
		if (icon == null) synchronized (this) {
			if ((icon = stackingIcon) == null) stackingIcon = icon = config.stackingIcon();
		}
		return icon;
	}
	private boolean isOptionalTaskReminderEnabled(String taskName) {
		if (SalveData.BLUE_DRAGON_TASK.equals(taskName)) return features.blueDragonTasks;
		if (SalveData.SKELETON_TASK.equals(taskName)) return features.skeletonTasks;
		return SalveData.OGRE_TASK.equals(taskName) && features.ogreTasks;
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
		if (!taskNameKnown) setCurrentTaskName(configManager.getRSProfileConfiguration(SLAYER_PLUGIN_GROUP, SLAYER_TASK_NAME_KEY));
		return currentTaskName;
	}
	private void setCurrentTaskName(String taskName) {
		taskNameKnown = true;
		if (taskName == null || (taskName = taskName.trim()).isEmpty()) {
			currentTaskName = null;
			return;
		}
		currentTaskName = taskName.toLowerCase(Locale.ROOT);
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
		for (String wildcard : ignoredNpcWildcards) if (matchesWildcard(wildcard, targetNameNormalized)) return true;
		return false;
	}
	private void updateTargetName(NPC npc, int npcId) {
		if (npcId != targetId) {
			String nameRaw = getNpcNameRaw(npc);
			targetId = npcId;
			targetNameCached = nameRaw == null ? null : Text.removeTags(nameRaw);
			targetNameNormalized = targetNameCached == null ? null : normalizePlain(targetNameCached);
		}
		targetName = targetNameCached;
	}
	private void resetAlert() {
		tooltipReason = null;
		targetName = null;
		alertItemId = NO_ICON;
		alertSpriteId = NO_ICON;
		alertType = AlertType.NONE;
	}
	private void resetTransientAlert() {
		transientTooltipReason = null;
		transientItemId = NO_ICON;
		transientSpriteId = NO_ICON;
		transientAlertType = AlertType.NONE;
	}
	private void resetTarget() {
		lastTarget = null;
		clearTargetCache();
		targetName = null;
		ticksSinceInteractionEnd = -1;
	}
	private boolean isTransientTaskAlert() {
		return transientAlertType == AlertType.USELESS_TASK || transientAlertType == AlertType.SALVE_TASK || transientAlertType == AlertType.TASK_OPTION;
	}
	private void clearTargetCache() {
		targetId = -1;
		targetNameCached = null;
		targetNameNormalized = null;
	}
	private void updateIgnoredNpcs() {
		if (lastIgnoredNpcs == null) updateIgnoredNpcs(config.ignoredNpcs());
	}
	private void updateIgnoredNpcs(String ignoredNpcs) {
		if (ignoredNpcs == null) ignoredNpcs = "";
		if (ignoredNpcs.equals(lastIgnoredNpcs)) return;
		if (ignoredNpcs.isEmpty()) {
			lastIgnoredNpcs = "";
			ignoredNpcNames = Set.of();
			ignoredNpcWildcards = Set.of();
			return;
		}
		Set<String> names = null;
		Set<String> wildcards = null;
		for (String entry : splitConfigList(ignoredNpcs)) {
			String name = normalize(entry);
			if (name.isEmpty()) continue;
			if (name.indexOf('*') != -1 || name.indexOf('?') != -1) {
				if (wildcards == null) wildcards = new HashSet<>();
				wildcards.add(name);
			} else {
				if (names == null) names = new HashSet<>();
				names.add(name);
			}
		}
		lastIgnoredNpcs = ignoredNpcs;
		ignoredNpcNames = names == null ? Set.of() : names;
		ignoredNpcWildcards = wildcards == null ? Set.of() : wildcards;
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
				return getDisplayIconId();
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
		return normalizePlain(Text.removeTags(text));
	}
	private static String normalizePlain(String text) {
		return text.replace('\u00A0', ' ').trim().toLowerCase(Locale.ROOT);
	}
	static boolean matchesWildcard(String wildcard, String text) {
		int wildcardIndex = 0;
		int textIndex = 0;
		int starIndex = -1;
		int starTextIndex = -1;
		while (textIndex < text.length()) {
			if (wildcardIndex < wildcard.length() && (wildcard.charAt(wildcardIndex) == '?' || wildcard.charAt(wildcardIndex) == text.charAt(textIndex))) {
				wildcardIndex++;
				textIndex++;
			} else if (wildcardIndex < wildcard.length() && wildcard.charAt(wildcardIndex) == '*') {
				starIndex = wildcardIndex++;
				starTextIndex = textIndex;
			} else if (starIndex != -1) {
				wildcardIndex = starIndex + 1;
				textIndex = ++starTextIndex;
			} else return false;
		}
		while (wildcardIndex < wildcard.length() && wildcard.charAt(wildcardIndex) == '*') wildcardIndex++;
		return wildcardIndex == wildcard.length();
	}
	private static String getNpcNameRaw(NPC npc) {
		NPCComposition composition = npc.getTransformedComposition();
		if (composition != null) {
			String name = composition.getName();
			if (name != null) return name;
		}
		return npc.getName();
	}
}
