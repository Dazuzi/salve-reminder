package com.salvereminder;
import com.salvereminder.data.SalveData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.*;
import java.awt.*;
@ConfigGroup("salvereminder")
public interface SalveReminderConfig extends Config {
	@RequiredArgsConstructor
	enum SalveIcon {
		SALVE_AMULET("Salve amulet", ItemID.CRYSTALSHARD_NECKLACE),
		SALVE_AMULET_E("Salve amulet (e)", ItemID.LOTR_CRYSTALSHARD_NECKLACE_UPGRADE),
		SALVE_AMULET_EI("Salve amulet (ei)", ItemID.NZONE_SALVE_AMULET_E);
		private final String name;
		@Getter
		private final int itemID;
		@Override
		public String toString() {
			return name;
		}
	}
	@RequiredArgsConstructor
	enum StackingIcon {
		AUTO("Auto equipped", -1),
		BLACK_MASK("Black mask", ItemID.SW_BLACK_MASK),
		SLAYER_HELMET("Slayer helmet", ItemID.SLAYER_HELM),
		BLACK_SLAYER_HELMET("Black Slayer", ItemID.SLAYER_HELM_BLACK),
		GREEN_SLAYER_HELMET("Green Slayer", ItemID.SLAYER_HELM_GREEN),
		RED_SLAYER_HELMET("Red Slayer", ItemID.SLAYER_HELM_RED),
		PURPLE_SLAYER_HELMET("Purple Slayer", ItemID.SLAYER_HELM_PURPLE),
		TURQUOISE_SLAYER_HELMET("Turquoise Slayer", ItemID.SLAYER_HELM_TURQUOISE),
		HYDRA_SLAYER_HELMET("Hydra Slayer", ItemID.SLAYER_HELM_HYDRA),
		TWISTED_SLAYER_HELMET("Twisted Slayer", ItemID.SLAYER_HELM_TWISTED),
		TZTOK_SLAYER_HELMET("TzTok Slayer", ItemID.SLAYER_HELM_JAD),
		VAMPYRIC_SLAYER_HELMET("Vampyric Slayer", ItemID.SLAYER_HELM_VERZIK),
		TZKAL_SLAYER_HELMET("TzKal Slayer", ItemID.SLAYER_HELM_ZUK),
		ARAXYTE_SLAYER_HELMET("Araxyte Slayer", ItemID.SLAYER_HELM_ARAXYTE),
		HOODED_SLAYER_HELMET("Hooded Slayer", ItemID.SLAYER_HELM_HOODED),
		OATHPLATE_SLAYER_HELMET("Oathplate Slayer", SalveData.OATHPLATE_SLAYER_HELMET_I),
		RADIANT_SLAYER_HELMET("Radiant Slayer", SalveData.RADIANT_SLAYER_HELMET_I);
		private final String name;
		@Getter
		private final int itemID;
		@Override
		public String toString() {
			return name;
		}
	}
	@RequiredArgsConstructor
	enum DebugAlert {
		OFF("Off"),
		USELESS_TARGET("Useless target"),
		UNDEAD_TARGET("Undead target"),
		USELESS_TASK("Useless task"),
		SALVE_TASK("Salve task"),
		STACKING("Stacking"),
		VORKATH_TASK("Vorkath task"),
		VETION_TASK("Vet'ion task"),
		ZOGRE_TASK("Zogre task");
		private final String name;
		@Override
		public String toString() {
			return name;
		}
	}
	@ConfigItem(
			keyName = "alertOnUndeadCombat",
			name = "Alert on Salve targets",
			description = "Alerts when Salve amulet is effective for an undead target or slayer task.",
			position = 0
	)
	default boolean alertOnUndeadCombat() { return true; }
	@ConfigItem(
			keyName = "showStackingWarning",
			name = "Stacking warning",
			description = "Warns you when wearing both a Slayer helmet/Black mask and a Salve amulet, as their effects do not stack.",
			position = 1
	)
	default boolean showStackingWarning() { return true; }
	@ConfigItem(
			keyName = "warnOnUselessSalve",
			name = "Warn on useless Salve",
			description = "Warns when wearing a Salve amulet when it is not effective.",
			position = 2
	)
	default boolean warnOnUselessSalve() { return true; }
	@ConfigItem(
			keyName = "ignoredNpcs",
			name = "Ignored NPCs",
			description = "NPC names to ignore. Supports * and ? wildcards. Separate with commas or new lines.",
			position = 3
	)
	default String ignoredNpcs() { return ""; }
	@ConfigItem(
			keyName = "ignoredNpcs",
			name = "",
			description = ""
	)
	void setIgnoredNpcs(String ignoredNpcs);
	@ConfigSection(
			name = "Slayer task reminders",
			description = "Settings for showing a reminder during an undead slayer task.",
			position = 4
	)
	String slayerTaskSection = "slayerTaskSection";
	@ConfigItem(
			keyName = "remindOnBlueDragonsTask",
			name = "Blue dragons (Vorkath)",
			description = "Show the reminder on a Blue dragons task, as killing Vorkath is an option.",
			section = slayerTaskSection,
			position = 0
	)
	default boolean remindOnBlueDragonsTask() { return true; }
	@ConfigItem(
			keyName = "remindOnSkeletonsTask",
			name = "Skeletons (Vet'ion/Calvar'ion)",
			description = "Show the reminder on a Skeletons task, as killing Vet'ion or Calvar'ion is an option.",
			section = slayerTaskSection,
			position = 1
	)
	default boolean remindOnSkeletonsTask() { return true; }
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
			position = 5
	)
	String appearanceSection = "appearanceSection";
	@ConfigSection(
			name = "Tweaks",
			description = "Extra tools for testing and tuning.",
			position = 6,
			closedByDefault = true
	)
	String tweaksSection = "tweaksSection";
	@ConfigItem(
			keyName = "displayIcon",
			name = "Salve amulet icon",
			description = "The Salve amulet icon to show in the reminder overlay.",
			section = appearanceSection,
			position = 0
	)
	default SalveIcon displayIcon() { return SalveIcon.SALVE_AMULET_EI; }
	@ConfigItem(
			keyName = "stackingIcon",
			name = "Stacking icon",
			description = "The Slayer helmet or Black mask icon to show for stacking warnings.",
			section = appearanceSection,
			position = 1
	)
	default StackingIcon stackingIcon() { return StackingIcon.AUTO; }
	@ConfigItem(
			keyName = "flashBackground",
			name = "Flash background",
			description = "Toggles the flashing of the overlay's background.",
			section = appearanceSection,
			position = 2
	)
	default boolean flashBackground() { return true; }
	@Alpha
	@ConfigItem(
			keyName = "backgroundColor",
			name = "Background color",
			description = "The background color of the reminder overlay.",
			section = appearanceSection,
			position = 3
	)
	default Color backgroundColor() { return new Color(36, 33, 30, 150); }
	@Alpha
	@ConfigItem(
			keyName = "flashBackgroundColor",
			name = "Flash background color",
			description = "The color the background will flash to.",
			section = appearanceSection,
			position = 4
	)
	default Color flashBackgroundColor() { return new Color(150, 0, 0, 150); }
	@ConfigItem(
			keyName = "hideAlertDelay",
			name = "Hide delay (ticks)",
			description = "How many ticks the reminder stays visible after combat ends.",
			section = appearanceSection,
			position = 5
	)
	@Range(max = 512)
	default int hideAlertDelay() { return 10; }
	@ConfigItem(
			keyName = "debugAlert",
			name = "Debug alert",
			description = "Temporarily displays a selected alert state.",
			section = tweaksSection,
			position = 0
	)
	default DebugAlert debugAlert() { return DebugAlert.OFF; }
}
