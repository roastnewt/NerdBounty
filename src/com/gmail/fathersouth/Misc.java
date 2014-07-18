package com.gmail.fathersouth;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class Misc {

    public static ItemStack createSkull(String victim, String killer, int skullID)
    {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(victim);
        if (killer != null) {
            List<String> lore = new ArrayList<String>();
            if (meta.hasLore()) lore = meta.getLore();
            lore.add(ChatColor.AQUA + "Killed by " + killer);
            lore.add(ChatColor.DARK_GRAY + "Skull #" + skullID);
            meta.setLore(lore);
        }
        skull.setItemMeta(meta);

        return skull;
    }

    public static void sendHalp(CommandSender sender) {

        sender.sendMessage(ChatColor.AQUA + "---- Bounty ----");
        sender.sendMessage(ChatColor.GRAY + "Place an item bounty on another player's head.");
        sender.sendMessage(ChatColor.GRAY + "Anyone can redeem that player's head for the bounty, and the head is given to the bounty issuer!");
        sender.sendMessage(ChatColor.GREEN + "/bounty add [PlayerName]");
        sender.sendMessage(ChatColor.GRAY + " - Places a bounty on [PlayerName]'s head, using the items in your hand");
        sender.sendMessage(ChatColor.GREEN + "/bounty list [PlayerName]");
        sender.sendMessage(ChatColor.GRAY + " - Lists all the bounties on [PlayerName]");
        sender.sendMessage(ChatColor.GREEN + "/bounty me ");
        sender.sendMessage(ChatColor.GRAY + " - Lists just the bounties on your head");
        sender.sendMessage(ChatColor.GREEN + "/bounty redeem ");
        sender.sendMessage(ChatColor.GRAY + " - Redeems the skull in your hand for its bounty");
        sender.sendMessage(ChatColor.GREEN + "/bounty getheads ");
        sender.sendMessage(ChatColor.GRAY + " - Get any player heads owed to you (after your bounty is redeemed)");
        sender.sendMessage(ChatColor.GREEN + "/bounty recent ");
        sender.sendMessage(ChatColor.GRAY + " - Lists recently issued bounties");
        sender.sendMessage(ChatColor.GREEN + "/bounty info [bounty#]");
        sender.sendMessage(ChatColor.GRAY + " - More info about a bounty (item durability, name, enchantments, etc)");

    }


}
