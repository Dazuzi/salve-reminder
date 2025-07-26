package com.salvereminder.core;

import com.salvereminder.SalveReminderConfig;
import com.salvereminder.data.SalveData;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReminderManager {
    private static final int FLASH_DURATION_TICKS = 1;
    private static final int EQUIPMENT_CONTAINER_ID = 94;
    private static final String SLAYER_PLUGIN_GROUP = "slayer";
    private static final String SLAYER_TASK_NAME_KEY = "taskName";
    @Inject
    @SuppressWarnings("unused")
    private Client client;
    @Inject
    @SuppressWarnings("unused")
    private EventBus eventBus;
    @Inject
    @SuppressWarnings("unused")
    private SalveReminderConfig config;
    @Inject
    @SuppressWarnings("unused")
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
    private Actor lastTarget = null;
    private int flashTickCounter = 0;
    private int ticksSinceInteractionEnd = -1;
    public void start() {
        eventBus.register(this);
    }
    public void stop() {
        eventBus.unregister(this);
        reset();
    }
    private void reset() {
        resetAlert();
        lastTarget = null;
        ticksSinceInteractionEnd = -1;
    }
    private void resetAlert() {
        showAlert = false;
        tooltipReason = null;
        conflictingHeadgearId = -1;
        isStackingWarningActive = false;
    }
    @Subscribe
    @SuppressWarnings("unused")
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            reset();
        }
    }
    @Subscribe
    @SuppressWarnings("unused")
    public void onInteractingChanged(InteractingChanged event) {
        if (event.getSource() != client.getLocalPlayer()) {
            return;
        }
        Actor target = event.getTarget();
        if (target != null) {
            lastTarget = target;
            ticksSinceInteractionEnd = -1;
        } else {
            if (lastTarget != null) {
                ticksSinceInteractionEnd = 0;
            }
        }
    }
    @Subscribe
    @SuppressWarnings("unused")
    public void onGameTick(GameTick tick) {
        if (ticksSinceInteractionEnd != -1) {
            ticksSinceInteractionEnd++;
        }
        updateAlertState();
        updateFlash();
    }
    private void updateAlertState() {
        if (lastTarget == null || (ticksSinceInteractionEnd != -1 && ticksSinceInteractionEnd > config.hideAlertDelay())) {
            reset();
            if (ticksSinceInteractionEnd > config.hideAlertDelay()) {
                lastTarget = null;
            }
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
        boolean isUndead = SalveData.UNDEAD_NPCS.contains(npcId);
        boolean isRelevantSlayerTask = isSlayerTaskReminderActive();
        boolean wearingSalve = isWearingSalveAmulet();
        int wornSlayerHelmId = getWornSlayerHelmOrBlackMaskId();
        boolean wearingSlayerHelm = wornSlayerHelmId != -1;
        if (config.showStackingWarning() && wearingSalve && wearingSlayerHelm && (isUndead || isRelevantSlayerTask)) {
            showAlert = true;
            tooltipReason = "Salve amulet and Black mask/Slayer helmet effects do not stack.";
            conflictingHeadgearId = wornSlayerHelmId;
            isStackingWarningActive = true;
            return;
        }
        if (config.warnOnUselessSalve() && wearingSalve && !isUndead) {
            showAlert = true;
            tooltipReason = "Salve amulet is ineffective against non-undead monsters.";
            return;
        }
        if (!wearingSalve && (isRelevantSlayerTask || (config.alertOnUndeadCombat() && isUndead))) {
            showAlert = true;
            if (isRelevantSlayerTask && !isUndead) {
                tooltipReason = "On a slayer task where Salve amulet is effective.";
            } else {
                tooltipReason = "Attacking an undead monster without Salve amulet.";
            }
            return;
        }
        resetAlert();
    }
    private void updateFlash() {
        if (!showAlert) {
            flash = false;
            flashTickCounter = 0;
            return;
        }
        flashTickCounter++;
        if (flashTickCounter >= FLASH_DURATION_TICKS) {
            flash = !flash;
            flashTickCounter = 0;
        }
    }
    private int getWornSlayerHelmOrBlackMaskId() {
        ItemContainer equipment = client.getItemContainer(EQUIPMENT_CONTAINER_ID);
        if (equipment == null) {
            return -1;
        }
        Item helmet = equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
        if (helmet == null) {
            return -1;
        }
        int helmetId = helmet.getId();
        if (SalveData.BLACK_MASK_IDS.contains(helmetId) || SalveData.SLAYER_HELMET_IDS.contains(helmetId)) {
            return helmetId;
        }
        return -1;
    }
    private boolean isSlayerTaskReminderActive() {
        if (!config.slayerTaskReminderEnabled()) {
            return false;
        }
        String taskName = configManager.getRSProfileConfiguration(SLAYER_PLUGIN_GROUP, SLAYER_TASK_NAME_KEY);
        if (taskName == null || taskName.isEmpty()) {
            return false;
        }
        taskName = taskName.toLowerCase();
        if (SalveData.MANDATORY_SLAYER_TASKS.contains(taskName)) {
            return true;
        }
        if (config.remindOnBlackDemonsTask() && SalveData.BLACK_DEMON_TASKS.contains(taskName)) {
            return true;
        }
        if (config.remindOnBlueDragonsTask() && SalveData.BLUE_DRAGON_TASKS.contains(taskName)) {
            return true;
        }
        if (config.remindOnMonkeysTask() && SalveData.MONKEY_TASKS.contains(taskName)) {
            return true;
        }
        return config.remindOnOgresTask() && SalveData.OGRE_TASKS.contains(taskName);
    }
    private boolean isWearingSalveAmulet() {
        ItemContainer equipment = client.getItemContainer(EQUIPMENT_CONTAINER_ID);
        if (equipment == null) {
            return false;
        }
        Item amulet = equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
        return amulet != null && SalveData.SALVE_AMULET_IDS.contains(amulet.getId());
    }
}