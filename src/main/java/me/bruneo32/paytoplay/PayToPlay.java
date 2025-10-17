package me.bruneo32.paytoplay;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;

import me.bruneo32.paytoplay.PayToPlayConfig;

import me.bruneo32.paytoplay.commands.*;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;

public class PayToPlay extends JavaPlugin implements Listener {

	public final static String accounting_csv_file = "accounting.csv";

	public PayToPlayConfig config = new PayToPlayConfig();
	public ConcurrentHashMap<String, Double> accounting = new ConcurrentHashMap<String, Double>();

	private File accountingCsv;
	private BukkitRunnable task;

	@Override
	public void onDisable() {
		if (task != null)
			task.cancel();
	}

	@Override
	public void onEnable() {
		/* Load config */
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists())
			saveDefaultConfig();

		config.hoursPerCharge  = getConfig().getDouble("PayToPlay.HoursPerCharge", 14);
		config.amountPerCharge = getConfig().getDouble("PayToPlay.AmountPerCharge", 0);
		config.debtToKick      = getConfig().getDouble("PayToPlay.DebtToKick", 3);
		config.workerTicks     = Math.max(1, getConfig().getInt("PayToPlay.WorkerTicks", 1200));

		String currencyStr  = getConfig().getString("PayToPlay.CurrencyChar", "$");
		config.currencyChar = currencyStr.isEmpty() ? '$' : currencyStr.charAt(0);

		/* Load accounting.csv */
		accountingCsv = new File(getDataFolder(), accounting_csv_file);
		if (!accountingCsv.exists()) {
			try { accountingCsv.createNewFile(); }
			catch (IOException e) {
				getLogger().severe("Could not create " + accounting_csv_file);
			}
		}

		/* Register commands */
		getCommand("owe").setExecutor(new OweCommand(this));

		/* Register events */
		getServer().getPluginManager().registerEvents(this, this);

		startWorker();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		kickPlayerIfOwesTooMuch(event.getPlayer());
	}

	public void readCSV() {
		try (
			Scanner file = new Scanner(accountingCsv);
		) {
			while (file.hasNextLine()) {
				String line = "";
				try {
					line = file.nextLine();
					String[] splitted = line.split(",");
					if (splitted.length != 2)
						throw new Exception("Invalid line: " + line);
					/* Add to map */
					double amount = Double.parseDouble(splitted[1].trim());
					accounting.put(splitted[0].trim().toLowerCase(), amount);
				} catch (NumberFormatException e) {
					getLogger().severe("Malformed field for line: " + line);
				} catch (Exception e) {
					getLogger().severe("Error parsing line: " + line + "\n" + e.getMessage());
				}
			}
		} catch (IOException e) {
			getLogger().severe("Could not read " + accounting_csv_file + "\n" + e.getMessage());
			return;
		} catch (Exception e) {
			getLogger().severe("Exception in Scanner: " + "\n" + e.getMessage());
			return;
		}
	}

	public void startWorker() {
		if (task != null) { return; }

		task = new BukkitRunnable() {
			@Override
			public void run() {
				/* Update accounting */
				readCSV();

				/* Make array copy to save concurrency segfaults */
				for (Player player : List.copyOf(Bukkit.getOnlinePlayers()))
					kickPlayerIfOwesTooMuch(player);
			}
		};

		task.runTaskTimer(this, 0, config.workerTicks);
		getLogger().info("Worker started!");
	}

	public double getPlayerTotalHours(OfflinePlayer player) {
		int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
		return ticks / 20.0 / 3600.0;
	}

	public double getPlayerTotalCharged(OfflinePlayer player) {
		double hours = getPlayerTotalHours(player);
		double charges = Math.floor(hours / config.hoursPerCharge);
		return charges * config.amountPerCharge;
	}

	public double getPlayerTotalPaid(OfflinePlayer player) {
		return accounting.getOrDefault(player.getName().toLowerCase(), 0.0);
	}

	public double getPlayerDebt(OfflinePlayer player) {
		double spent = getPlayerTotalCharged(player);
		double paid  = getPlayerTotalPaid(player);

		double amountDebt = spent - paid;
		if (amountDebt < 0)
			return 0;
		return amountDebt;
	}

	public OfflinePlayer getPlayerFromName(String target) {
		target = target.trim();

		/* Try online player exact match first */
		Player online = Bukkit.getPlayerExact(target);
		if (online != null)
			return (OfflinePlayer)online;

		/* Try searching for offline player */
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			String name = player.getName();
			if (name != null && name.equalsIgnoreCase(target))
				return player;
		}

		return null;
	}

	public void kickPlayerIfOwesTooMuch(Player player) {
		double debt = getPlayerDebt(player);
		if (debt >= config.debtToKick) {
			player.kickPlayer(
				"You have a debt of " + String.format("%.2f%c", debt, config.currencyChar) +
				"\nIn order to continue using this server you have to pay your current debt.");
			/* Log & broadcast message */
			String kickString = String.format("Kicked %s due to debt %.2f%c", player.getName(), debt, config.currencyChar);
			getLogger().info(kickString);
			Bukkit.broadcastMessage(kickString);
		}
	}

}
