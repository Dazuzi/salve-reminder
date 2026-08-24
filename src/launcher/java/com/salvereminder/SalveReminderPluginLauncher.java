package com.salvereminder;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
public class SalveReminderPluginLauncher {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin(SalveReminderPlugin.class);
		RuneLite.main(args);
	}
}
