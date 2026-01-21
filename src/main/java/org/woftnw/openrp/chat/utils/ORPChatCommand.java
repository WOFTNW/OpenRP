package org.woftnw.openrp.chat.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ORPChatCommand extends Command {

	public ORPChatCommand(String name, List<String> aliases) {
		super(name);
		this.setAliases(aliases);
		this.setDescription(name + " channel");
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String label, String @NotNull [] args) {

		List<String> suggestions = new ArrayList<>();

		if (args.length <= 1) {
			suggestions.add("<message>");
		}

		return suggestions;

	}

}
