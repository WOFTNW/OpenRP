package org.woftnw.openrp.chat.listeners;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.woftnw.openrp.OpenRP;
import org.woftnw.openrp.chat.events.ORPChatEvent;
import org.woftnw.openrp.chat.events.ORPChatPreprintEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A listener built into OpenRP that handles toggling and switching channels.
 * This is due to the large amount of requests for doing something like this.
 *
 * @author Darwin Jonathan
 */
public class ToggleSwitchListener implements Listener {

	private final OpenRP plugin;

	private static Map<Player, List<String>> toggles;
	private static Map<Player, String> switches;

	private static List<String> toggleChannels;
	private static List<String> switchChannels;

	private static boolean useToggles;
	private static boolean useSwitches;

	public ToggleSwitchListener(@NotNull OpenRP plugin) {
		this.plugin = plugin;
		toggles = new HashMap<>();
		switches = new HashMap<>();
		File file = new File(plugin.getDataFolder() + File.separator + "chat", "toggle-and-switcher.yml");
		if (!file.exists()) {
			plugin.saveResource("chat/toggle-and-switcher.yml", false);
		}
		FileConfiguration configfile = YamlConfiguration.loadConfiguration(file);
		useToggles = configfile.getBoolean("enable-channel-toggler", false);
		useSwitches = configfile.getBoolean("enable-channel-switcher", false);
		toggleChannels = configfile.isSet("toggleable-channels") ? configfile.getStringList("toggleable-channels")
				: new ArrayList<String>();
		switchChannels = configfile.isSet("switchable-channels") ? configfile.getStringList("switchable-channels")
				: new ArrayList<String>();
	}

	public static boolean usingToggles() {
		return useToggles;
	}

	public static boolean usingSwitches() {
		return useSwitches;
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull List<String> getToggleChannels() {
		return new ArrayList<>(toggleChannels);
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull List<String> getSwitchChannels() {
		return new ArrayList<>(switchChannels);
	}

	public static void setToggleList(Player p, List<String> l) {
		toggles.put(p, l);
	}

	public static List<String> getToggleList(Player p) {
		return toggles.containsKey(p) ? toggles.get(p) : new ArrayList<>();
	}

	@EventHandler
	public void onToggledChannelMessage(ORPChatPreprintEvent event) {
		if (!useToggles) {
			return;
		}
		if (!toggles.containsKey(event.getPlayer())) {
			return;
		}
		if (!toggles.get(event.getPlayer()).contains(event.getChannel())) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onToggledOffChannelMessage(ORPChatEvent event) {
		if (!useToggles) {
			return;
		}
		if (!toggles.containsKey(event.getPlayer())) {
			return;
		}
		if (!toggles.get(event.getPlayer()).contains(event.getChannel())) {
			return;
		}
		event.getPlayer().sendMessage(plugin.getChat().getMessage("cant-use-when-toggled-off"));
		event.setCancelled(true);
	}

	public static void setSwitchChannel(Player p, String c) {
		switches.put(p, c);
	}

	public static String getSwitchChannel(Player p) {
		return switches.getOrDefault(p, null);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerLogin(PlayerJoinEvent event) {
		if (!useSwitches) {
			return;
		}
		setSwitchChannel(event.getPlayer(), plugin.getChat().getConfig().getString("default"));
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onSwitchedChannelMessage(ORPChatEvent event) {
		if (!useSwitches) {
			return;
		}
		if (event.wasCommand()) {
			return;
		}
		if (!switches.containsKey(event.getPlayer())) {
			return;
		}
		String channel = switches.get(event.getPlayer());
		// Check permission node
		if (plugin.getChat().getConfig().isSet("channels." + channel + ".use-perm")) {
			if (!event.getPlayer()
					.hasPermission(plugin.getChat().getConfig().getString("channels." + channel + ".use-perm"))) {
				event.getPlayer().sendMessage(plugin.getChat().getMessage("no-use-perm"));
				event.setCancelled(true);
			}
		}
		// Check cooldown
		if (!event.getPlayer().hasPermission(plugin.getChat().getConfig().getString("bypass-cooldown-perm"))) {
			if (!plugin.getChat().getCooldowns().isEmpty()) {
				if (plugin.getChat().getCooldowns().containsKey(event.getPlayer())) {
					if (!plugin.getChat().getCooldowns().get(event.getPlayer()).isEmpty()) {
						if (plugin.getChat().getCooldowns().get(event.getPlayer()).containsKey(channel)) {
							if ((System.currentTimeMillis()
									- plugin.getChat().getCooldowns().get(event.getPlayer()).get(channel))
									/ 1000 < plugin.getChat().getConfig().getInt("channels." + channel + ".cooldown")) {
								event.getPlayer().sendMessage(plugin.getChat().getMessage("cooldown").replace("{time}",
										((Long) (plugin.getChat().getConfig()
												.getInt("channels." + channel + ".cooldown")
												- (System.currentTimeMillis() - plugin.getChat().getCooldowns()
												.get(event.getPlayer()).get(channel)) / 1000)).toString()));
								event.setCancelled(true);
							}
						}
					}

				}
			}
		}
		// Ok
		event.setChannel(channel);
	}

}
