package openrp.time;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import openrp.OpenRP;
import openrp.time.cmds.Command_ROLEPLAYTIME;
import openrp.time.events.ORPTimeProcessEvent;
import openrp.time.utils.TimeHandler;
import org.jetbrains.annotations.NotNull;

/**
 * OpenRP Time API instance. Can be accessed from the OpenRP main class via
 * getTime().
 *
 * @author Darwin Jonathan
 */
public class ORPTime {

	private final OpenRP plugin;
	private File directory;

	private static final long HOUR_0 = 18000;
	private final ArrayList<TimeHandler> times = new ArrayList<>();
	private HashMap<World, BukkitTask> scheduleTracker = new HashMap<>();

	private FileConfiguration config;
	private FileConfiguration messages;
	private FileConfiguration timedata;

	public ORPTime(OpenRP plugin) {
		this.plugin = plugin;
	}

	/**
	 * Register all times for OpenRP Time to be able to use.
	 */
	public void registerTimes() {

		times.clear();

		for (World world : plugin.getServer().getWorlds()) {

			boolean skip = false;
			if (getConfig().isSet("disabled-worlds")) {
				if (getConfig().getStringList("disabled-worlds").contains(world.getName())) {
					plugin.getLogger().info("World " + world.getName() + " ignores Time. Skipping. . .");
					skip = true;
				}
			}

			if (!skip) {

				if (getTimedata().contains(world.getName())) {
					times.add(new TimeHandler(plugin, world, getTimedata().getInt(world.getName() + ".second"),
							getTimedata().getInt(world.getName() + ".minute"), getTimedata().getInt(world.getName() + ".hour"),
							getTimedata().getInt(world.getName() + ".day"), getTimedata().getInt(world.getName() + ".month"),
							getTimedata().getInt(world.getName() + ".year")));
				} else {
					if (getConfig().isSet("default-time")) {
						times.add(new TimeHandler(plugin, world, getConfig().getInt("default-time.second"),
								getConfig().getInt("default-time.minute"), getConfig().getInt("default-time.hour"),
								getConfig().getInt("default-time.day"), getConfig().getInt("default-time.month"),
								getConfig().getInt("default-time.year")));
					}
				}

				plugin.getLogger().info("Added " + world.getName() + " to Time.");
				if (getConfig().getBoolean("handle-time")) {
					if (world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)) {
						plugin.getLogger().warning("Gamerule doDaylightCycle for " + world.getName()
								+ " is set to true. Please make sure it's set to false, or change 'handle-time' to false in config!");
					}
				}

			}

		}

	}

	/**
	 * Get all of OpenRP Time's times for all registered worlds.
	 *
	 * @return An ArrayList TimeHandler objects for each registered world.
	 */
	public ArrayList<TimeHandler> getTimes() {
		return times;
	}

	/**
	 * Restarts the time handler for OpenRP Time. This handles all the time
	 * operations, including updating times, sending actionbars, and
	 * changing the worlds' times if needed.
	 */
	public void restartTimeHandler() {

		if (!scheduleTracker.isEmpty()) {
			for (World world : scheduleTracker.keySet()) {
				scheduleTracker.get(world).cancel();
			}
			scheduleTracker = new HashMap<>();
		}

		for (TimeHandler timeHandler : getTimes()) {

			if (!plugin.getServer().getWorlds().contains(timeHandler.getWorld())) {
				continue;
			}

			scheduleTracker.put(timeHandler.getWorld(), plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {

				int second = timeHandler.getSecond();
				int minute = timeHandler.getMinute();
				int hour = timeHandler.getHour();
				int day = timeHandler.getDay();
				int month = timeHandler.getMonth();
				int year = timeHandler.getYear();

				boolean longMonth = month == 1 || month == 3 || month == 5 || month == 7 || month == 8
						|| month == 10 || month == 12;
				boolean leapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
				if (getConfig().getBoolean("handle-time")) {

					second += (int) (double) (getConfig().getInt("run-time-event-every-in-ticks")
							/ getConfig().getInt("one-second-in-ticks-is"));

					if (second >= 60) {

						second = second - 60;
						minute++;

						if (minute >= 60) {

							minute = 0;
							hour++;

							if (hour >= 24) {

								hour = 0;
								day++;

								if (longMonth) {
									if (day >= 32) {

										day = 1;
										month++;

										if (month >= 13) {

											month = 1;
											year++;

										}

									}
								} else if (month == 4 || month == 6 || month == 9 || month == 11) {
									if (day >= 31) {

										day = 1;
										month++;

									}
								} else if (month == 2) {
									if (leapYear) {
										if (day >= 30) {

											day = 1;
											month++;

										}
									} else {
										if (day >= 29) {

											day = 1;
											month++;

										}
									}
								}

							}

						}

					}

				} else {

					second = (int) Math.floor(((timeHandler.getWorld().getTime() / 1000.0) % 1 * 60) % 1 * 60);

					minute = (int) Math.floor((timeHandler.getWorld().getTime() / 1000.0) % 1 * 60);

					hour = (int) (6 + timeHandler.getWorld().getTime() / 1000);
					if (hour >= 24) {
						hour = hour - 24;
					}

					if (hour < timeHandler.getHour()) {

						day++;

						if (longMonth) {
							if (day >= 32) {

								day = 1;
								month++;

								if (month >= 13) {

									month = 1;
									year++;

								}

							}
						} else if (month == 4 || month == 6 || month == 9 || month == 11) {
							if (day >= 31) {

								day = 1;
								month++;

							}
						} else if (month == 2) {
							if (leapYear) {
								if (day >= 30) {

									day = 1;
									month++;

								}
							} else {
								if (day >= 29) {

									day = 1;
									month++;

								}
							}
						}

					}

				}

				ORPTimeProcessEvent event = new ORPTimeProcessEvent(timeHandler.getWorld(), second, minute, hour, day, month,
						year);
				plugin.getServer().getPluginManager().callEvent(event);

				timeHandler.setSecond(event.getSecond());
				timeHandler.setMinute(event.getMinute());
				timeHandler.setHour(event.getHour());
				timeHandler.setDay(event.getDay());
				timeHandler.setMonth(event.getMonth());
				timeHandler.setYear(event.getYear());

				if (getConfig().isSet("format")) {
					for (Player player : timeHandler.getWorld().getPlayers()) {
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
								TextComponent.fromLegacyText(
										plugin.colorize(plugin.parsePlaceholders(getConfig().getString("format"), player,
												getStandardHashMap(player, timeHandler.getSecond(), timeHandler.getMinute(), timeHandler.getHour(),
														timeHandler.getDay(), timeHandler.getMonth(), timeHandler.getYear())), false)));
					}
				}

				if (getConfig().getBoolean("handle-time")) {
					timeHandler.getWorld().setTime(
							calculateWorldTimeFromHandlerTime(timeHandler.getHour(), timeHandler.getMinute(), timeHandler.getSecond()));
				}

			}, 0L, getConfig().getInt("run-time-event-every-in-ticks")));

		}

	}

	/**
	 * A convenient method to convert hour:minute:second time that is used in OpenRP
	 * Time to ticks.
	 *
	 * @return The world's time in ticks.
	 */
	public long calculateWorldTimeFromHandlerTime(int hour, int minute, int second) {
		long time = ORPTime.HOUR_0;
		time += 1000L * hour;
		time += Math.round(16.6666 * minute);
		time += Math.round(0.2777 * second);
		if (time >= 24000) {
			time = time - 24000;
		}
		return time;
	}

	/**
	 * Returns the day of the week as text from the current day in the month of the
	 * specific year.
	 *
	 * @param day   - the day to calculate for.
	 * @param month - the month to calculate for.
	 * @param year  - the year to calculate for.
	 * @return A String representing the day of the week as text.
	 */
	public String getDayFromNumber(int day, int month, int year) {
		Calendar c = Calendar.getInstance();
		c.set(year, month - 1, day);
		switch (c.get(Calendar.DAY_OF_WEEK)) {
			case 1:
				return getConfig().getString("days.sunday");
			case 3:
				return getConfig().getString("days.tuesday");
			case 4:
				return getConfig().getString("days.wednesday");
			case 5:
				return getConfig().getString("days.thursday");
			case 6:
				return getConfig().getString("days.friday");
			case 7:
				return getConfig().getString("days.saturday");
			default:
				return getConfig().getString("days.monday");
		}
	}

	/**
	 * Returns the month as text.
	 *
	 * @param month - the month to calculate for
	 * @return A String representing the month as text.
	 */
	public String getMonthFromNumber(int month) {
		switch (month) {
			case 2:
				return getConfig().getString("months.february");
			case 3:
				return getConfig().getString("months.march");
			case 4:
				return getConfig().getString("months.april");
			case 5:
				return getConfig().getString("months.may");
			case 6:
				return getConfig().getString("months.june");
			case 7:
				return getConfig().getString("months.july");
			case 8:
				return getConfig().getString("months.august");
			case 9:
				return getConfig().getString("months.september");
			case 10:
				return getConfig().getString("months.october");
			case 11:
				return getConfig().getString("months.november");
			case 12:
				return getConfig().getString("months.december");
			default:
				return getConfig().getString("months.january");
		}
	}

	/**
	 * Calls a HashMap with standard replacements for this plugin.
	 */
	public HashMap<String, String> getStandardHashMap(@NotNull Player player, Integer second, Integer minute, Integer hour,
													  Integer day, Integer month, Integer year) {
		HashMap<String, String> map = new HashMap<>();
		map.put("{player}", player.getName());
		if (second < 10) {
			map.put("{second}", "0" + second);
		} else {
			map.put("{second}", second.toString());
		}
		if (minute < 10) {
			map.put("{minute}", "0" + minute);
		} else {
			map.put("{minute}", minute.toString());
		}
		if (hour < 10) {
			map.put("{hour}", "0" + hour);
		} else {
			map.put("{hour}", hour.toString());
		}
		if (day < 10) {
			if (getConfig().getBoolean("day-as-words")) {
				map.put("{day}", getDayFromNumber(day, month, year));
			} else {
				map.put("{day}", "0" + day);
			}
		} else {
			if (getConfig().getBoolean("day-as-words")) {
				map.put("{day}", getDayFromNumber(day, month, year));
			} else {
				map.put("{day}", day.toString());
			}
		}
		if (month < 10) {
			if (getConfig().getBoolean("month-as-words")) {
				map.put("{month}", getMonthFromNumber(month));
			} else {
				map.put("{month}", "0" + month);
			}
		} else {
			if (getConfig().getBoolean("month-as-words")) {
				map.put("{month}", getMonthFromNumber(month));
			} else {
				map.put("{month}", month.toString());
			}
		}
		map.put("{rawday}", day.toString());
		map.put("{rawmonth}", month.toString());
		map.put("{year}", year.toString());
		return map;
	}

	/**
	 * Registers all event classes related to OpenRP Time.
	 */
	public void registerEvents() {
		plugin.getLogger().info("Registering Time Worlds...");
		plugin.getLogger().info("Registering Time Commands...");
		Command_ROLEPLAYTIME handler_TIME = new Command_ROLEPLAYTIME(plugin);
		plugin.getCommand("roleplaytime").setExecutor(handler_TIME);
		plugin.getCommand("roleplaytime").setTabCompleter(handler_TIME);
		plugin.getCommand("roleplaytime").setPermission(getConfig().getString("manage-perm"));
		// ensures proper load
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			registerTimes();
			restartTimeHandler();
		}, 5);
		plugin.getLogger().info("Time Loaded!");
	}

	/**
	 * If x/plugins/OpenRP/time does not exist, this creates it, to avoid problems.
	 */
	public void fixFilePath() {
		directory = new File(plugin.getDataFolder() + File.separator + "time");
		if (!directory.exists()) {
			if (!directory.mkdir()) plugin.getLogger().warning("Unable to create directory " + directory.getName() + ".");
		}
	}

	/**
	 * Reloads OpenRP Time's config.yml file.
	 */
	public void reloadConfig() {
		File file_config = new File(directory, "config.yml");
		if (!file_config.exists()) {
			plugin.saveResource("time/config.yml", false);
		}
		config = YamlConfiguration.loadConfiguration(file_config);
	}

	/**
	 * Saves OpenRP Time's config.yml file.
	 */
	public void saveConfig() {
		File file_config = new File(directory, "time.yml");
		try {
			config.save(file_config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns OpenRP Time's config.yml file for you to use.
	 */
	public FileConfiguration getConfig() {
		return config;
	}

	/**
	 * Reloads OpenRP Descriptions' messages.yml file.
	 */
	public void reloadMessages() {
		File file_messages = new File(directory, "messages.yml");
		if (!file_messages.exists()) {
			plugin.saveResource("time/messages.yml", false);
		}
		messages = YamlConfiguration.loadConfiguration(file_messages);
	}

	/**
	 * Returns OpenRP Descriptions' messages.yml file for you to use.
	 */
	public FileConfiguration getMessages() {
		return messages;
	}

	/**
	 * Is a quick shortcut that returns the colorized message from the messages
	 * file.
	 */
	public String getMessage(String path) {
		return plugin.colorize(getMessages().getString(path), false);
	}

	/**
	 * Reloads OpenRP Time's timedata.yml file.
	 */
	public void reloadTimedata() {
		File fileTimedata = new File(directory, "timedata.yml");
		if (!fileTimedata.exists()) {
			plugin.saveResource("time/timedata.yml", false);
		}
		timedata = YamlConfiguration.loadConfiguration(fileTimedata);
	}

	/**
	 * Saves OpenRP Time's timedata.yml file.
	 */
	public void saveTimedata() {
		File fileTimedata = new File(directory, "timedata.yml");
		try {
			timedata.save(fileTimedata);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns OpenRP Time's timedata.yml file for you to use.
	 */
	public FileConfiguration getTimedata() {
		return timedata;
	}

}
