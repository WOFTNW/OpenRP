package openrp.descriptions.cmds;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import openrp.OpenRP;
import openrp.descriptions.events.ORPDescriptionsChangeEvent;
import org.jetbrains.annotations.NotNull;

public class Command_CHARACTER implements CommandExecutor, TabCompleter {

	private final OpenRP plugin;

	public Command_CHARACTER(OpenRP plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String @NotNull [] args) {

		if (args.length == 0) {

			if (!(sender instanceof Player)) {
				sender.sendMessage(plugin.getDesc().getMessage("run-as-player"));
				return true;
			}

			for (String field : plugin.getDesc().getFields()) {

				for (String t : plugin.getDesc().getMessages().getStringList("field-manipulation.field-set-format")) {

					if (!t.contains("{change}")) {

						sender.sendMessage(plugin.colorize(
								t.replace("{field}", field).replace("{value}",
										plugin.getDesc().getUserdata()
												.getString(((Player) sender).getUniqueId().toString() + "." + field)),
								false));

					} else {

						ArrayList<TextComponent> msg = new ArrayList<>();
						for (String u : t.split(" ")) {

							if (u.equals("{change}")) {

								if (plugin.getDesc().getConfig().getString("fields." + field + ".allowed-values.type")
										.equalsIgnoreCase("list")) {

									for (String w : plugin.getDesc().getConfig()
											.getStringList("fields." + field + ".allowed-values.value")) {

										String v = ChatColor.stripColor(plugin.colorize(w, true));

										TextComponent add = new TextComponent(TextComponent.fromLegacyText(
												plugin.getDesc().getMessage("field-manipulation.change.list")
														.replace("{value}", v)));

										add.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
												new ComponentBuilder(plugin.colorize(plugin.getDesc().getMessages()
														.getString("field-manipulation.hover", "&a&lO"), false))
														.create()));
										add.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
												"/character set " + field + " " + v));

										msg.add(add);
									}

								} else if (plugin.getDesc().getConfig()
										.getString("fields." + field + ".allowed-values.type").equalsIgnoreCase("contains")
										|| plugin.getDesc().getConfig()
										.getString("fields." + field + ".allowed-values.type")
										.equalsIgnoreCase("without")) {

									TextComponent add = new TextComponent(TextComponent.fromLegacyText(
											plugin.getDesc().getMessage("field-manipulation.change.single")));

									add.setHoverEvent(
											new HoverEvent(HoverEvent.Action.SHOW_TEXT,
													new ComponentBuilder(plugin.colorize(plugin.getDesc().getMessages()
															.getString("field-manipulation.hover", "&a&lO"), false))
															.create()));
									add.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
											"/character set " + field + " "));

									msg.add(add);

								} else if (plugin.getDesc().getConfig()
										.getString("fields." + field + ".allowed-values.type").equalsIgnoreCase("locked")) {

									TextComponent add = new TextComponent(TextComponent.fromLegacyText(
											plugin.getDesc().getMessage("field-manipulation.change.locked")));

									msg.add(add);

								}

							}

						}

						TextComponent send = new TextComponent();
						for (TextComponent add : msg) {
							send.addExtra(add);
						}
						((Player) sender).spigot().sendMessage(send);

					}

				}
			}

		} else {

			if (args[0].equalsIgnoreCase("override")) {

				if (!sender.hasPermission(plugin.getDesc().getConfig().getString("override-perm"))) {
					sender.sendMessage(plugin.getDesc().getMessage("no-perm"));
					return true;
				}

				if (args.length < 4) {
					sender.sendMessage(plugin.getDesc().getMessage("invalid-use").replace("{usage}",
							label + " override (user) (field) (value)"));
					return true;
				}

				String field = "";
				for (String s : plugin.getDesc().getFields()) {
					if (s.equalsIgnoreCase(args[2])) {
						field = s;
					}
				}
				if (field.isEmpty()) {
					sender.sendMessage(plugin.getDesc().getMessage("invalid-field"));
					return true;
				}

				String suuid = "undefined";
				String name = args[1];
				if (plugin.getServer().getPlayer(args[1]) == null) {
					for (OfflinePlayer o : plugin.getServer().getOfflinePlayers()) {
						if (o.getName().equals(args[1])) {
							suuid = o.getUniqueId().toString();
							name = o.getName();
							break;
						}
					}
				} else {
					suuid = plugin.getServer().getPlayer(args[1]).getUniqueId().toString();
					name = plugin.getServer().getPlayer(args[1]).getName();
				}

				if (suuid.equalsIgnoreCase("undefined")) {
					sender.sendMessage(plugin.getDesc().getMessage("invalid-player"));
					return true;
				}

				StringBuilder value = new StringBuilder();
				for (String s : args) {
					value.append(" ").append(plugin.colorize(s, false));
				}
				value = new StringBuilder(value.toString().replaceFirst(plugin.colorize(" " + args[0] + " " + args[1] + " " + args[2] + " ", false),
						""));
				ORPDescriptionsChangeEvent event = new ORPDescriptionsChangeEvent(UUID.fromString(suuid), field, value.toString());
				plugin.getServer().getPluginManager().callEvent(event);

				if (!event.isCancelled()) {

					field = event.getField();
					value = new StringBuilder(event.getValue());

					plugin.getDesc().setField(UUID.fromString(suuid), value.toString(), field);
					sender.sendMessage(plugin.getDesc().getMessage("set.override").replace("{player}", name)
							.replace("{field}", field).replace("{value}", value.toString()));

				}

			} else if (args[0].equalsIgnoreCase("check")) {

				if (!(sender instanceof Player)) {
					sender.sendMessage(plugin.getDesc().getMessage("run-as-player"));
					return true;
				}

				if (!sender.hasPermission(plugin.getDesc().getConfig().getString("check-perm"))) {
					sender.sendMessage(plugin.getDesc().getMessage("no-perm"));
					return true;
				}

				if (args.length != 2) {
					sender.sendMessage(
							plugin.getDesc().getMessage("invalid-use").replace("{usage}", label + " check (user)"));
					return true;
				}

				Player player = (Player) sender;

				String suuid = "undefined";
				if (plugin.getServer().getPlayer(args[1]) == null) {
					for (OfflinePlayer o : plugin.getServer().getOfflinePlayers()) {
						if (o.getName().equals(args[1])) {
							suuid = o.getUniqueId().toString();
							break;
						}
					}
				} else {
					suuid = plugin.getServer().getPlayer(args[1]).getUniqueId().toString();
				}

				if (suuid.equalsIgnoreCase("undefined")) {
					sender.sendMessage(plugin.getDesc().getMessage("invalid-player"));
					return true;
				}

				for (String s : plugin.getDesc().getConfig().getStringList("description-format")) {
					player.sendMessage(plugin.colorize(plugin.parsePlaceholders(s, UUID.fromString(suuid),
							plugin.getDesc().getStandardHashMap(UUID.fromString(suuid))), false));
				}

			} else if (args[0].equalsIgnoreCase("set")) {

				if (!(sender instanceof Player)) {
					sender.sendMessage(plugin.getDesc().getMessage("run-as-player"));
					return true;
				}

				Player player = (Player) sender;

				if (!player.hasPermission(plugin.getDesc().getConfig().getString("use-perm"))) {
					player.sendMessage(plugin.getDesc().getMessage("no-perm"));
					return true;
				}

				if (args.length < 3) {
					player.sendMessage(plugin.getDesc().getMessage("invalid-use").replace("{usage}",
							label + " set (field) (value)"));
					return true;
				}

				String field = "";
				for (String s : plugin.getDesc().getFields()) {
					if (s.equalsIgnoreCase(args[1])) {
						field = s;
					}
				}
				if (field.isEmpty()) {
					player.sendMessage(plugin.getDesc().getMessage("invalid-field"));
					return true;
				}

				StringBuilder value = new StringBuilder();
				boolean hasPerm = false;
				if (plugin.getDesc().getConfig().isSet("fields." + field + ".color-code-perm")) {
					hasPerm = player.hasPermission(
							plugin.getDesc().getConfig().getString("fields." + field + ".color-code-perm"));
				}
				for (String arg : args) {
					value.append(" ").append(plugin.colorize(arg, !hasPerm));
				}
				value = new StringBuilder(value.toString().replaceFirst(plugin.colorize(" " + args[0] + " " + args[1] + " ", !hasPerm), ""));
				ORPDescriptionsChangeEvent event = new ORPDescriptionsChangeEvent(player.getUniqueId(), field, value.toString());
				plugin.getServer().getPluginManager().callEvent(event);

				if (!event.isCancelled()) {

					field = event.getField();
					value = new StringBuilder(event.getValue());

					if (plugin.getDesc().getCooldowns().containsKey(player)) {
						if (!player.hasPermission(plugin.getDesc().getConfig().getString("bypass-cooldown-perm"))) {
							if (TimeUnit.MILLISECONDS.toSeconds(
									System.currentTimeMillis() - plugin.getDesc().getCooldowns().get(player)) < plugin
									.getDesc().getConfig().getInt("fields." + field + ".cooldown")) {
								player.sendMessage(plugin.getDesc().getMessage("cooldown").replace("{time}",
										((Long) (plugin.getDesc().getConfig().getInt("fields." + field + ".cooldown")
												- TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()
												- plugin.getDesc().getCooldowns().get(player)))).toString()));
								return true;
							}
						}
					}

					plugin.getDesc().getCooldowns().put(player, System.currentTimeMillis());

					if (plugin.getDesc().getConfig().isSet("fields." + field + ".color-code-perm")) {
						if (!hasPerm) {
							value = new StringBuilder(ChatColor.stripColor(value.toString()));
						}
					}

					if (plugin.getDesc().getConfig().isSet("fields." + field + ".length.minimum")) {
						if (plugin.getDesc().getConfig().getInt("fields." + field + ".length.minimum") > ChatColor
								.stripColor(value.toString()).length()) {
							player.sendMessage(plugin.getDesc().getMessage("invalid-length.min").replace("{len}",
									plugin.getDesc().getConfig().getString("fields." + field + ".length.minimum")));
							return true;
						}
					}

					if (plugin.getDesc().getConfig().isSet("fields." + field + ".length.maximum")) {
						if (plugin.getDesc().getConfig().getInt("fields." + field + ".length.maximum") < ChatColor
								.stripColor(value.toString()).length()) {
							player.sendMessage(plugin.getDesc().getMessage("invalid-length.max").replace("{len}",
									plugin.getDesc().getConfig().getString("fields." + field + ".length.maximum")));
							return true;
						}
					}

					if (plugin.getDesc().getConfig().isSet("fields." + field + ".allowed-values.type")) {
						if (plugin.getDesc().getConfig().getString("fields." + field + ".allowed-values.type")
								.equalsIgnoreCase("contains")) {

							if (plugin.getDesc().getConfig().isSet("fields." + field + ".allowed-values.value")) {

								boolean caseSensitive = false;

								if (plugin.getDesc().getConfig()
										.isSet("fields." + field + ".allowed-values.case-sensitive")) {
									if (plugin.getDesc().getConfig()
											.getBoolean("fields." + field + ".allowed-values.case-sensitive")) {
										caseSensitive = true;
									}
								}

								if (!caseSensitive) {
									for (String s : ChatColor.stripColor(value.toString()).split("")) {
										if (!plugin.getDesc().getConfig()
												.getString("fields." + field + ".allowed-values.value").toLowerCase()
												.contains(s.toLowerCase())) {
											StringBuilder val = new StringBuilder();
											for (String t : plugin.getDesc().getConfig()
													.getString("fields." + field + ".allowed-values.value")
													.toLowerCase().split("")) {
												val.append(", ").append(t);
											}
											val = new StringBuilder(val.toString().replaceFirst(", ", ""));
											player.sendMessage(plugin.getDesc().getMessage("not-allowed.contains")
													.replace("{val}", val.toString()));
											return true;
										}
									}
								} else {
									for (String s : ChatColor.stripColor(value.toString()).split("")) {
										if (!plugin.getDesc().getConfig()
												.getString("fields." + field + ".allowed-values.value").contains(s)) {
											StringBuilder val = new StringBuilder();
											for (String t : plugin.getDesc().getConfig()
													.getString("fields." + field + ".allowed-values.value").split("")) {
												val.append(", ").append(t);
											}
											val = new StringBuilder(val.toString().replaceFirst(", ", ""));
											player.sendMessage(plugin.getDesc().getMessage("not-allowed.contains")
													.replace("{val}", val.toString()));
											return true;
										}
									}
								}

							}

						} else if (plugin.getDesc().getConfig().getString("fields." + field + ".allowed-values.type")
								.equalsIgnoreCase("without")) {

							if (plugin.getDesc().getConfig().isSet("fields." + field + ".allowed-values.value")) {

								boolean caseSensitive = false;

								if (plugin.getDesc().getConfig()
										.isSet("fields." + field + ".allowed-values.case-sensitive")) {
									if (plugin.getDesc().getConfig()
											.getBoolean("fields." + field + ".allowed-values.case-sensitive")) {
										caseSensitive = true;
									}
								}

								if (!caseSensitive) {
									for (String s : ChatColor.stripColor(value.toString()).split("")) {
										if (plugin.getDesc().getConfig()
												.getString("fields." + field + ".allowed-values.value").toLowerCase()
												.contains(s.toLowerCase())) {
											StringBuilder val = new StringBuilder();
											for (String t : plugin.getDesc().getConfig()
													.getString("fields." + field + ".allowed-values.value")
													.toLowerCase().split("")) {
												val.append(", ").append(t);
											}
											val = new StringBuilder(val.toString().replaceFirst(", ", ""));
											player.sendMessage(plugin.getDesc().getMessage("not-allowed.contains")
													.replace("{val}", val.toString()));
											return true;
										}
									}
								} else {
									for (String s : ChatColor.stripColor(value.toString()).split("")) {
										if (plugin.getDesc().getConfig()
												.getString("fields." + field + ".allowed-values.value").contains(s)) {
											StringBuilder val = new StringBuilder();
											for (String t : plugin.getDesc().getConfig()
													.getString("fields." + field + ".allowed-values.value").split("")) {
												val.append(", ").append(t);
											}
											val = new StringBuilder(val.toString().replaceFirst(", ", ""));
											player.sendMessage(plugin.getDesc().getMessage("not-allowed.contains")
													.replace("{val}", val.toString()));
											return true;
										}
									}
								}

							}

						} else if (plugin.getDesc().getConfig().getString("fields." + field + ".allowed-values.type")
								.equalsIgnoreCase("list")) {

							if (plugin.getDesc().getConfig().isSet("fields." + field + ".allowed-values.value")) {

								boolean caseSensitive = false;

								if (plugin.getDesc().getConfig()
										.isSet("fields." + field + ".allowed-values.case-sensitive")) {
									if (plugin.getDesc().getConfig()
											.getBoolean("fields." + field + ".allowed-values.case-sensitive")) {
										caseSensitive = true;
									}
								}

								boolean found = false;

								if (!caseSensitive) {
									for (String s : plugin.getDesc().getConfig()
											.getStringList("fields." + field + ".allowed-values.value")) {
										if (ChatColor.stripColor(plugin.colorize(s, true))
												.equalsIgnoreCase(ChatColor.stripColor(plugin.colorize(value.toString(), true)))) {
											value = new StringBuilder(plugin.colorize(s, false));
											found = true;
											break;
										}
									}
								} else {
									for (String s : plugin.getDesc().getConfig()
											.getStringList("fields." + field + ".allowed-values.value")) {
										if (ChatColor.stripColor(plugin.colorize(s, true))
												.equals(ChatColor.stripColor(plugin.colorize(value.toString(), true)))) {
											value = new StringBuilder(plugin.colorize(s, false));
											found = true;
											break;
										}
									}
								}

								if (!found) {
									StringBuilder val = new StringBuilder();
									for (String s : plugin.getDesc().getConfig()
											.getStringList("fields." + field + ".allowed-values.value")) {
										val.append(", ").append(s);
									}
									val = new StringBuilder(val.toString().replaceFirst(", ", ""));
									player.sendMessage(
											plugin.getDesc().getMessage("not-allowed.list").replace("{val}", val.toString()));
									return true;
								}

							}

						} else if (plugin.getDesc().getConfig().getString("fields." + field + ".allowed-values.type")
								.equalsIgnoreCase("locked")) {

							player.sendMessage(plugin.getDesc().getMessage("not-allowed.locked"));
							return true;

						}
					}

					plugin.getDesc().setField(player, value.toString(), field);
					player.sendMessage(plugin.getDesc().getMessage("set.self").replace("{field}", field)
							.replace("{value}", plugin.colorize(value.toString(), false)));

				}

			} else if (args[0].equalsIgnoreCase("profile")) {

				if (!(sender instanceof Player)) {
					sender.sendMessage(plugin.getDesc().getMessage("run-as-player"));
					return true;
				}

				Player p = (Player) sender;

				if (!p.hasPermission(plugin.getDesc().getConfig().getString("profile-perm", "orpdesc.profile"))) {
					p.sendMessage(plugin.getDesc().getMessage("no-perm"));
					return true;
				}

				if (args.length < 2) {
					p.sendMessage(plugin.getDesc().getMessage("profile-usage", ChatColor.RED + "Please provide save, use or delete!"));
					return true;
				}

				String action = args[1];

				if (args.length < 3) {
					p.sendMessage(plugin.getDesc().getMessage("profile-require-name",
							ChatColor.RED + "Please provide a name for this profile!"));
					return true;
				}

				String profile = args[2];
				if (action.equalsIgnoreCase("save")) {
					ConfigurationSection profiles = plugin.getDesc().getUserdata()
							.getConfigurationSection(p.getUniqueId().toString() + ".profiles");
					if (profiles != null
							&& !p.hasPermission(plugin.getConfig().getString("bypass-max-profiles-perm",
							"orpdesc.bypassmaxprofiles"))
							&& profiles.getKeys(false).size() >= plugin.getDesc().getConfig().getInt("max-profiles", 5)
							&& plugin.getDesc().getConfig().getInt("max-profiles", 5) != -1) {
						p.sendMessage(plugin.getDesc().getMessage("profile-max-reached",
								ChatColor.RED + "You can't have any more profiles!"));
						return true;
					}

					ConfigurationSection fields = plugin.getDesc().getUserdata()
							.getConfigurationSection(p.getUniqueId().toString());
					Map<String, Object> map = new HashMap<>();
					for (String field : fields.getKeys(false)) {
						if (!field.equalsIgnoreCase("profiles"))
							map.put(field, fields.get(field));
					}
					plugin.getDesc().getUserdata().set(p.getUniqueId().toString() + ".profiles." + profile, map);
					plugin.getDesc().saveUserdata();
					plugin.getDesc().reloadUserdata();
					p.sendMessage(plugin.getDesc().getMessage("profile-saved", ChatColor.GREEN + "Profile {profile} saved!")
							.replace("{profile}", profile));

				} else if (action.equalsIgnoreCase("use")) {
					ConfigurationSection fields = plugin.getDesc().getUserdata()
							.getConfigurationSection(p.getUniqueId().toString() + ".profiles." + profile);
					if (fields != null) {
						for (String field : fields.getKeys(false))
							plugin.getDesc().setField(p.getUniqueId(), fields.getString(field), field);
						p.sendMessage(plugin.getDesc().getMessage("profile-changed", ChatColor.GREEN + "Now using {profile}!")
								.replace("{profile}", profile));

					} else
						p.sendMessage(
								plugin.getDesc().getMessage("profile-not-found", ChatColor.RED + "The profile {profile} doesn't exist!")
										.replace("{profile}", profile));

				} else if (action.equalsIgnoreCase("delete")) {
					plugin.getDesc().getUserdata().set(p.getUniqueId().toString() + ".profiles." + profile, null);
					plugin.getDesc().saveUserdata();
					p.sendMessage(plugin.getDesc().getMessage("profile-deleted", ChatColor.YELLOW + "Profile {profile} deleted!")
							.replace("{profile}", profile));
				}

			} else {
				sender.sendMessage(plugin.getDesc().getMessage("invalid-use").replace("{usage}", label));
			}

		}

		return true;

	}

	@Override
	public List<String> onTabComplete(CommandSender sender, @NotNull Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("character")) {
			if (args.length <= 1) {

				List<String> l = new ArrayList<String>();
				if (sender.hasPermission(plugin.getDesc().getConfig().getString("use-perm"))) {
					l.add("set");
				}
				if (sender.hasPermission(plugin.getDesc().getConfig().getString("override-perm"))) {
					l.add("override");
				}
				if (sender.hasPermission(plugin.getDesc().getConfig().getString("check-perm"))) {
					l.add("check");
				}
				if (sender.hasPermission(plugin.getDesc().getConfig().getString("profile-perm", "orpdesc.profile"))) {
					l.add("profile");
				}

				return l;

			} else if (args.length == 2) {

				List<String> l = new ArrayList<>();
				if (args[0].equalsIgnoreCase("set")) {
					if (sender.hasPermission(plugin.getDesc().getConfig().getString("use-perm"))) {
						l.addAll(plugin.getDesc().getFields());
					}
				} else if (args[0].equalsIgnoreCase("override")) {
					if (sender.hasPermission(plugin.getDesc().getConfig().getString("override-perm"))) {
						return null;
					}
				} else if (args[0].equalsIgnoreCase("check")) {
					if (sender.hasPermission(plugin.getDesc().getConfig().getString("check-perm"))) {
						return null;
					}
				} else if (args[0].equalsIgnoreCase("profile")) {
					if (sender
							.hasPermission(plugin.getDesc().getConfig().getString("profile-perm", "orpdesc.profile"))) {
						l.addAll(Arrays.asList("use", "save", "delete"));
					}
				}

				return l;

			} else if (args.length == 3) {

				List<String> l = new ArrayList<String>();
				if (args[0].equalsIgnoreCase("set")) {
					if (sender.hasPermission(plugin.getDesc().getConfig().getString("use-perm"))) {
						l.add("<value>");
					}
				} else if (args[0].equalsIgnoreCase("override")) {
					if (sender.hasPermission(plugin.getDesc().getConfig().getString("override-perm"))) {
						l.addAll(plugin.getDesc().getFields());
					}
				} else if (args[0].equalsIgnoreCase("profile") && !args[1].equalsIgnoreCase("save")) {
					if (sender
							.hasPermission(plugin.getDesc().getConfig().getString("profile-perm", "orpdesc.profile"))) {
						ConfigurationSection profiles = plugin.getDesc().getUserdata()
								.getConfigurationSection(((Player) sender).getUniqueId().toString() + ".profiles");
						if (profiles != null)
							l.addAll(profiles.getKeys(false));
					}
				}

				return l;

			} else if (args.length == 4) {

				List<String> l = new ArrayList<>();
				if (args[0].equalsIgnoreCase("override")) {
					if (sender.hasPermission(plugin.getDesc().getConfig().getString("override-perm"))) {
						l.add("<value>");
					}
				}

				return l;

			} else {

				return new ArrayList<>();

			}
		}

		return null;

	}

}
