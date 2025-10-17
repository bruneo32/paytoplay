package me.bruneo32.paytoplay.commands;

import me.bruneo32.paytoplay.PayToPlay;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OweCommand implements CommandExecutor {

	private final PayToPlay plugin;
	private final Logger logger;

	public OweCommand(PayToPlay plugin) {
		this.plugin = plugin;
		this.logger = plugin.getLogger();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		OfflinePlayer p;
		if (args.length > 0) {
			p = plugin.getPlayerFromName(args[0].trim());
		} else if (sender instanceof Player) {
			p = (OfflinePlayer)sender;
		} else {
			sender.sendMessage("You must be a player to check your debt!");
			return false;
		}

		if (p == null || !p.hasPlayedBefore()) {
			sender.sendMessage("Player not found!");
			return false;
		}

		double debt = plugin.getPlayerDebt(p);
		double hours = plugin.getPlayerTotalHours(p);
		double spent = plugin.getPlayerTotalCharged(p);
		double paid  = plugin.getPlayerTotalPaid(p);

		StringBuilder sb = new StringBuilder();

		/* Header */
		sb.append(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "[PayToPlay] " + ChatColor.AQUA + "Debt Information\n");
		sb.append(ChatColor.GRAY + "--------------------------------------------\n");

		/* Main debt summary */
		sb.append(
			ChatColor.YELLOW + "" + ChatColor.BOLD + p.getName() + ChatColor.RESET +
			ChatColor.GRAY + " owes " +
			ChatColor.GOLD + "" + ChatColor.UNDERLINE + String.format("%.2f%c", debt, plugin.config.currencyChar)
			+ "\n\n");

		/* Details section */
		sb.append(ChatColor.DARK_GRAY + "Details:\n");
		sb.append(
			ChatColor.GRAY + " • " + ChatColor.GREEN + "Playtime: " +
			ChatColor.WHITE + String.format("%.2f", hours) + ChatColor.GRAY
			+ (hours == 1 ? " hour" : " hours")
			+ "\n");
		sb.append(
			ChatColor.GRAY + " • " + ChatColor.RED + "Total Spent: " +
			ChatColor.GOLD + String.format("%.2f%c", spent, plugin.config.currencyChar)
			+ "\n");
		sb.append(
			ChatColor.GRAY + " • " + ChatColor.GREEN + "Total Paid: " +
			ChatColor.AQUA + String.format("%.2f%c", paid, plugin.config.currencyChar)
			+ "\n\n");

		/* Rate info */
		sb.append(
			ChatColor.GRAY + "Current rate: " +
			ChatColor.YELLOW + String.format("%.2f%c", plugin.config.amountPerCharge, plugin.config.currencyChar) +
			ChatColor.GRAY + " per " +
			ChatColor.WHITE + String.format("%.2f", plugin.config.hoursPerCharge) +
			ChatColor.GRAY + (plugin.config.hoursPerCharge == 1 ? " hour" : " hours")
			+ "\n");

		sb.append(ChatColor.GRAY + "--------------------------------------------");
		sb.append(ChatColor.RESET);

		sender.sendMessage(sb.toString());

		return true;
	}
}
