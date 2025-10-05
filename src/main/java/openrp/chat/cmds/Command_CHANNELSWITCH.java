package openrp.chat.cmds;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import openrp.OpenRP;
import openrp.chat.listeners.ToggleSwitchListener;
import org.jetbrains.annotations.NotNull;

public class Command_CHANNELSWITCH implements CommandExecutor, TabCompleter {

	private final OpenRP plugin;

	private final String defaultChannel;

	public Command_CHANNELSWITCH(@NotNull OpenRP plugin) {
		this.plugin = plugin;
		defaultChannel = plugin.getChat().getConfig().getString("default", null);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(plugin.getChat().getMessage("run-as-player"));
			return true;
		}

		if (!ToggleSwitchListener.usingSwitches()) {
			sender.sendMessage(plugin.getChat().getMessage("toggle-switch-disabled"));
			return true;
		}

		if (args.length != 1) {
			sender.sendMessage(plugin.getChat().getMessage("invalid-use").replace("{usage}", label + " (channel)"));
			return true;
		}

		if (!ToggleSwitchListener.getSwitchChannels().contains(args[0]) && !args[0].equals(defaultChannel)) {
			sender.sendMessage(plugin.getChat().getMessage("invalid-channel").replace("{channels}",
					ToggleSwitchListener.getSwitchChannels().toString().replace("[", "").replace("]", "")));
			return true;
		}
		if (plugin.getChat().getConfig().isSet("channels." + args[0] + ".use-perm")) {
			if (!sender.hasPermission(plugin.getChat().getConfig().getString("channels." + args[0] + ".use-perm"))) {
				sender.sendMessage(plugin.getChat().getMessage("no-use-perm"));
				return true;
			}
		}
		if (ToggleSwitchListener.getSwitchChannel((Player) sender) != null) {
			if (ToggleSwitchListener.getSwitchChannel((Player) sender).equals(args[0])) {
				sender.sendMessage(plugin.getChat().getMessage("already-switched"));
				return true;
			}
		} else {
			if (args[0].equals(defaultChannel)) {
				sender.sendMessage(plugin.getChat().getMessage("already-switched"));
				return true;
			}
		}

		ToggleSwitchListener.setSwitchChannel((Player) sender, args[0]);
		sender.sendMessage(plugin.getChat().getMessage("switched").replace("{channel}", args[0]));

		return true;
	}

	private final List<String> emptyArrayList = new ArrayList<>();

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String @NotNull [] args) {
		if (args.length <= 1) {
			List<String> l = ToggleSwitchListener.getSwitchChannels();
			l.add(defaultChannel);
			return l;
		}
		return emptyArrayList;
	}

}
