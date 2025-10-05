package openrp;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import openrp.descriptions.events.ORPDescriptionsChangeEvent;
import org.jetbrains.annotations.NotNull;

public class Command_OPENRP implements CommandExecutor, TabCompleter {

	private final OpenRP plugin;

	public Command_OPENRP(OpenRP plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String @NotNull [] args) {

		/*
		 * No longer required. See Command#setPermission(); if
		 * (!sender.hasPermission(plugin.getConfig().getString("admin-perm"))) {
		 * sender.sendMessage(plugin.getMessage("no-perm")); return true; }
		 */

		if (args.length == 0) {
			StringBuilder stringBuilder = new StringBuilder();
			for (String t : plugin.getConfig().getStringList("enabled")) {
				stringBuilder.append(", ").append(t.toLowerCase());
			}
			stringBuilder = new StringBuilder(stringBuilder.toString().replaceFirst(", ", ""));
			for (String t : plugin.getMessages().getStringList("openrp-command")) {
				sender.sendMessage(plugin
						.colorize(t.replace("{v}", plugin.getDescription().getVersion()).replace("{modules}", stringBuilder.toString()), false));

			}
			return true;
		} else if (args.length == 1) {
			if (args[0].equalsIgnoreCase("reload")) {
				sender.sendMessage(plugin.getMessage("reloading.all"));
				plugin.reloadConfig();
				plugin.reloadMessages();
				cmd.setPermission(plugin.getConfig().getString("admin-perm"));
				for (String s : plugin.getConfig().getStringList("enabled")) {
					if (s.equalsIgnoreCase("chat")) {
						plugin.getChat().reloadConfig();
						plugin.getChat().reloadMessages();
						plugin.getLogger().warning(
								"Newly added channel commands will not be visible until you restart and will look"
										+ ChatColor.RED + "RED" + ChatColor.RESET
										+ "! The commands will still work though.");
						plugin.getChat().registerChannels();
					} else if (s.equalsIgnoreCase("descriptions")) {
						plugin.getDesc().reloadConfig();
						plugin.getDesc().reloadMessages();
						plugin.getDesc().reloadUserdata();
						plugin.getDesc().registerFields();
						plugin.getLogger().info("Running Descriptions Default Info Completer...");
						plugin.getLogger().warning(
								"Reloading Descriptions with new fields added is not recommended, if errors occur please restart!");
						for (Player p : plugin.getServer().getOnlinePlayers()) {
							if (plugin.getDesc().getFields().isEmpty()) {
								break;
							}
							for (String t : plugin.getDesc().getFields()) {
								if (!plugin.getDesc().isFieldSet(p, t)) {
									String value = plugin.getDesc().getConfig()
											.getString("fields." + t + ".default-value");
									ORPDescriptionsChangeEvent changeevent = new ORPDescriptionsChangeEvent(
											p.getUniqueId(), t, value);
									if (!changeevent.isCancelled()) {
										plugin.getDesc().setField(p, changeevent.getValue(), changeevent.getField());
									}
								}
							}
						}
					} else if (s.equalsIgnoreCase("rolls")) {
						plugin.getRolls().reloadConfig();
						plugin.getRolls().reloadMessages();
					} else if (s.equalsIgnoreCase("time")) {
						plugin.getTime().reloadConfig();
						plugin.getTime().reloadMessages();
						plugin.getTime().saveTimedata();
						plugin.getTime().reloadTimedata();
						plugin.getTime().registerTimes();
						plugin.getTime().restartTimeHandler();
					}
				}
				sender.sendMessage(plugin.getMessage("reloading.done"));
			} else {
				sender.sendMessage(plugin.getMessage("invalid-use").replace("{usage}", label));
			}
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("reload")) {
				if (args[1].equalsIgnoreCase("chat")) {
					sender.sendMessage(plugin.getMessage("reloading.specific").replace("{m}", "chat"));
					plugin.getChat().reloadConfig();
					plugin.getChat().reloadMessages();
					plugin.getLogger()
							.warning("Newly added channel commands will not be visible until you restart and will look"
									+ ChatColor.RED + "RED" + ChatColor.RESET
									+ "! The commands will still work though.");
					plugin.getChat().registerChannels();
					sender.sendMessage(plugin.getMessage("reloading.done"));
				} else if (args[1].equalsIgnoreCase("descriptions")) {
					sender.sendMessage(plugin.getMessage("reloading.specific").replace("{m}", "descriptions"));
					plugin.getDesc().reloadConfig();
					plugin.getDesc().reloadMessages();
					plugin.getDesc().saveUserdata();
					plugin.getDesc().reloadUserdata();
					plugin.getDesc().registerFields();
					plugin.getLogger().warning(
							"Reloading Descriptions with modified fields added is not recommended, if errors occur please restart!");
					for (Player player : plugin.getServer().getOnlinePlayers()) {
						if (plugin.getDesc().getFields().isEmpty()) {
							break;
						}
						for (String field : plugin.getDesc().getFields()) {
							if (!plugin.getDesc().isFieldSet(player, field)) {
								String value = plugin.getDesc().getConfig().getString("fields." + field + ".default-value");
								ORPDescriptionsChangeEvent changeevent = new ORPDescriptionsChangeEvent(player.getUniqueId(),
										field, value);
								if (!changeevent.isCancelled()) {
									plugin.getDesc().setField(player, changeevent.getValue(), changeevent.getField());
								}
							}
						}
					}
					sender.sendMessage(plugin.getMessage("reloading.done"));
				} else if (args[1].equalsIgnoreCase("rolls")) {
					sender.sendMessage(plugin.getMessage("reloading.specific").replace("{m}", "rolls"));
					plugin.getRolls().reloadConfig();
					plugin.getRolls().reloadMessages();
					sender.sendMessage(plugin.getMessage("reloading.done"));
				} else if (args[1].equalsIgnoreCase("time")) {
					sender.sendMessage(plugin.getMessage("reloading.specific").replace("{m}", "time"));
					plugin.getTime().reloadConfig();
					plugin.getTime().reloadMessages();
					plugin.getTime().saveTimedata();
					plugin.getTime().reloadTimedata();
					plugin.getTime().registerTimes();
					plugin.getTime().restartTimeHandler();
					sender.sendMessage(plugin.getMessage("reloading.done"));
				} else {
					sender.sendMessage(plugin.getMessage("invalid-use").replace("{usage}",
							label + " reload (chat/descriptions/rolls/time)"));
				}
			} else {
				sender.sendMessage(plugin.getMessage("invalid-use").replace("{usage}", label));
			}
		} else {
			sender.sendMessage(plugin.getMessage("invalid-use").replace("{usage}", label));
		}

		return true;

	}

	@Override
	public List<String> onTabComplete(CommandSender sender, @NotNull Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("openrp")) {
			if (args.length == 1) {

				List<String> l = new ArrayList<>();
				if (sender.hasPermission(plugin.getConfig().getString("admin-perm"))) {
					l.add("reload");
				}
				return l;

			}
			if (args.length == 2) {

				List<String> suggestions = new ArrayList<>();
				if (sender.hasPermission(plugin.getConfig().getString("admin-perm"))) {
					suggestions.add("chat");
					suggestions.add("descriptions");
					suggestions.add("rolls");
					suggestions.add("time");
				}

				return suggestions;

			} else {

				return new ArrayList<String>();

			}
		}

		return null;

	}

}
