package com.salvereminder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("salvereminder")
public interface SalveReminderConfig extends Config {
    @Getter
    @RequiredArgsConstructor
    enum SalveIcon {
        @SuppressWarnings("unused")
        SALVE_AMULET("Salve amulet", 4081),
        @SuppressWarnings("unused")
        SALVE_AMULET_E("Salve amulet (e)", 10588),
        SALVE_AMULET_EI("Salve amulet (ei)", 12018);
        private final String name;
        private final int itemID;
        @Override
        public String toString() {
            return name;
        }
    }

    @ConfigItem(
            keyName = "alertOnUndeadCombat",
            name = "Alert while fighting undead",
            description = "Toggles the reminder when attacking undead monsters without a Salve Amulet.",
            position = 0
    )
    default boolean alertOnUndeadCombat() { return true; }

    @ConfigItem(
            keyName = "showStackingWarning",
            name = "Show Stacking Warning",
            description = "Warns you when wearing both a Slayer Helmet/Black Mask and a Salve Amulet, as their effects do not stack.",
            position = 1
    )
    default boolean showStackingWarning() { return true; }

    @ConfigItem(
            keyName = "warnOnUselessSalve",
            name = "Warn on useless Salve",
            description = "Warns you when wearing a Salve Amulet while fighting non-undead monsters.",
            position = 2
    )
    default boolean warnOnUselessSalve() { return true; }

    @ConfigSection(
            name = "Slayer Task Reminders",
            description = "Settings for showing the reminder during an undead slayer task.",
            position = 3
    )
    String slayerTaskSection = "slayerTaskSection";

    @ConfigItem(
            keyName = "slayerTaskReminderEnabled",
            name = "Enable Slayer Task Reminder",
            description = "Shows a reminder if you're wearing a slayer helmet or black mask on an undead task.",
            section = slayerTaskSection,
            position = 0
    )
    default boolean slayerTaskReminderEnabled() { return true; }

    @ConfigItem(
            keyName = "remindOnBlueDragonsTask",
            name = "Blue Dragons (Vorkath)",
            description = "Show the reminder on a Blue Dragons task, as killing Vorkath is an option.",
            section = slayerTaskSection,
            position = 1
    )
    default boolean remindOnBlueDragonsTask() { return true; }

    @ConfigItem(
            keyName = "remindOnOgresTask",
            name = "Ogres (Zogres/Skogres)",
            description = "Show the reminder on an Ogres task, as killing Zogres or Skogres is an option.",
            section = slayerTaskSection,
            position = 2
    )
    default boolean remindOnOgresTask() { return true; }

    @ConfigSection(
            name = "Appearance",
            description = "Settings for the reminder overlay's appearance.",
            position = 4
    )
    String appearanceSection = "appearanceSection";

    @ConfigItem(
            keyName = "displayIcon",
            name = "Display Icon",
            description = "The icon to display in the reminder overlay.",
            section = appearanceSection,
            position = 0
    )
    default SalveIcon displayIcon() { return SalveIcon.SALVE_AMULET_EI; }

    @ConfigItem(
            keyName = "flashBackground",
            name = "Flash Background",
            description = "Toggles the flashing of the overlay's background.",
            section = appearanceSection,
            position = 1
    )
    default boolean flashBackground() { return true; }

    @Alpha
    @ConfigItem(
            keyName = "backgroundColor",
            name = "Background Color",
            description = "The background color of the reminder overlay.",
            section = appearanceSection,
            position = 2
    )
    default Color backgroundColor() { return new Color(36, 33, 30, 150); }

    @Alpha
    @ConfigItem(
            keyName = "flashBackgroundColor",
            name = "Flash Background Color",
            description = "The color the background will flash to.",
            section = appearanceSection,
            position = 3
    )
    default Color flashBackgroundColor() { return new Color(150, 0, 0, 150); }

    @ConfigItem(
            keyName = "hideAlertDelay",
            name = "Hide Delay (ticks)",
            description = "How many ticks the reminder stays visible after combat ends.",
            section = appearanceSection,
            position = 4
    )
    @Range(max = 512)
    default int hideAlertDelay() { return 10; }
}