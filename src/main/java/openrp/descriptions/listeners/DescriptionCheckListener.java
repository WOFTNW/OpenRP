package openrp.descriptions.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import openrp.OpenRP;
import openrp.descriptions.events.ORPDescriptionsChangeEvent;
import org.jetbrains.annotations.NotNull;

public class DescriptionCheckListener implements Listener {

	private final OpenRP plugin;

	public DescriptionCheckListener(OpenRP plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerDescriptionCheck(@NotNull PlayerInteractEntityEvent event) {

		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
			return;
		}

		if (!(event.getRightClicked() instanceof Player)) {
			return;
		}
		Player clicked = (Player) event.getRightClicked();

		if (!(plugin.getServer().getOnlinePlayers().contains(clicked))) {
			return;
		}

		if (plugin.getDesc().getConfig().isSet("right-click-player-to-view-description")) {
			if (!plugin.getDesc().getConfig().getBoolean("right-click-player-to-view-description")) {
				return;
			}
		} else {
			return;
		}

		if (plugin.getDesc().getConfig().isSet("must-crouch-to-view-description")) {
			if (plugin.getDesc().getConfig().getBoolean("must-crouch-to-view-description")) {
				if (!event.getPlayer().isSneaking()) {
					return;
				}
			}
		}

		if (plugin.getDesc().getConfig().isSet("description-format")) {
			for (String s : plugin.getDesc().getConfig().getStringList("description-format")) {
				event.getPlayer()
						.sendMessage(plugin.colorize(plugin.parsePlaceholders(s, (Player) event.getRightClicked(),
								plugin.getDesc().getStandardHashMap((Player) event.getRightClicked())), false));
			}
		}

	}

	@EventHandler
	public void onPlayerJoinSetDefaultValues(PlayerJoinEvent event) {

		if (plugin.getDesc().getFields().isEmpty()) {
			return;
		}

		for (String field : plugin.getDesc().getFields()) {
			if (!plugin.getDesc().isFieldSet(event.getPlayer(), field)) {

				String value = plugin.getDesc().getConfig().getString("fields." + field + ".default-value");

				ORPDescriptionsChangeEvent changeevent = new ORPDescriptionsChangeEvent(event.getPlayer().getUniqueId(),
						field, value);

				if (!changeevent.isCancelled()) {
					plugin.getDesc().setField(event.getPlayer(), changeevent.getValue(), changeevent.getField());
				}

			}
		}

	}

}
