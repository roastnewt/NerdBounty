package com.gmail.fathersouth;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

//todo: enumerate prereqs for methods
//todo: comments?

// Known Issues:
// - I use Player.sendMessage(String) asynchronously all the time.  It's currently threadsafe, but there's
// no guarantee it will stay that way.
// - Multiple copies of the same item, but with different enchantments might group together in the /bounty info
// screen, instead of being listed separately
// - Potions all show up as Potion with a damage value - not names (i.e. Potion:8202 instead of Potion of Slowness)
// - Adding bounties too quickly will tell you to try again, slow down
// - Enchantment names are not very nice i.e. DAMAGE_ALL 5 rather than Sharpness 5.

public class NerdBounty extends JavaPlugin {

    private NerdBountyDAO _dao = new NerdBountyDAO(this);
    public Config config;
    public HashMap<UUID, Map.Entry<UUID, ItemStack>> _pendingBounties = new HashMap<UUID, Map.Entry<UUID, ItemStack>>();  //Key: IssuerUUID, Value: <TargetUUID, ItemStack>
    public NerdBounty _plugin = this;
    public HashMap<UUID, Long> _bountyAnnounceCooldowns = new HashMap<UUID, Long>();

    @Override
    public void onEnable() {

        // side effect: creates the NerdBounty directory for the SQLite db, so do this first.
        saveDefaultConfig();

        config = new Config(this);

        _dao.connect("localhost", "NerdBountyDB", "username", "password", this.getDataFolder());

        Bukkit.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                _dao.keepAlive();
            }
        }, 20L, 6000L);

    }

    @Override
    public void onDisable() {

        _dao.disconnect();

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("bounty")) {

            //
            // I'm just using the onCommand method to check prerequisites.  The "meat" of each command
            // is in the various CommandXXX() methods.
            //

            if (args.length == 0) {

                Misc.sendHalp(sender);
                return true;

            } else if (args[0].equalsIgnoreCase("add")) {

                if (args.length == 1) {

                    sender.sendMessage(ChatColor.GREEN + "/bounty add [PlayerName]");
                    sender.sendMessage(ChatColor.GRAY + "Takes the item in your hand, and places it as a bounty on the head of [PlayerName]");
                    return true;

                } else if (args.length > 2) {

                    sender.sendMessage(ChatColor.RED + "Too Many Arguments!  Usage: " + ChatColor.GREEN + "/bounty add [PlayerName]");
                    return true;

                } else if (!(sender instanceof Player)) {

                    sender.sendMessage(ChatColor.RED + "This command is for in-game players only!");
                    return true;

                } else {

                    String targetPlayerString = args[1];
                    commandAddBounty(sender, targetPlayerString);
                    return true;

                }

            } else if (args[0].equalsIgnoreCase("confirm")) {

                if (!(sender instanceof Player)) {

                    sender.sendMessage(ChatColor.RED + "This command is for in-game players only!");
                    return true;

                } else {

                    commandConfirmAddBounty(sender);
                    return true;

                }

            } else if (args[0].equalsIgnoreCase("redeem") || args[0].equalsIgnoreCase("claim")) {

                // redeeming a head for its bounty

                if (!(sender instanceof Player)) {

                    sender.sendMessage(ChatColor.RED + "This command is for in-game players only!");
                    return true;

                } else if (args.length == 1) {

                    commandListBountiesSkull(sender);
                    return true;

                } else if (args.length == 2) {

                    int bountyID;

                    final String bountyIDString = args[1];
                    try {
                        bountyID = Integer.valueOf(bountyIDString);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Please provide a bounty number to redeem!");
                        commandListBountiesSkull(sender);
                        return true;
                    }

                    commandRedeemSkullBounty(sender, bountyID);
                    return true;


                } else {

                    sender.sendMessage(ChatColor.RED + "Too Many Arguments!");
                    return true;

                }
            } else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("check")) {

                if (args.length == 1) {

                    sender.sendMessage(ChatColor.GRAY + "Type " + ChatColor.GREEN + "\"/bounty list [PlayerName]\" to check a player's bounty.");
                    sender.sendMessage(ChatColor.GRAY + "or type " + ChatColor.GREEN + "\"/bounty list head\" while holding a player's head.");
                    sender.sendMessage(ChatColor.GRAY + "Type " + ChatColor.GREEN + "\"/bounty recent\" to see a list of recently added bounties");
                    return true;

                } else if (args.length == 2) {
                    // check provided player
                    if (args[1].equalsIgnoreCase("head")) {

                        commandListBountiesSkull(sender);

                    } else {

                        commandListBountiesPlayer(sender, args[1]);

                    }
                    return true;

                } else {
                    sender.sendMessage(ChatColor.RED + "Too Many Arguments!");
                    return true;
                }

            } else if (args[0].equalsIgnoreCase("me")) {

                if (!(sender instanceof Player)) {

                    sender.sendMessage(ChatColor.RED + "This command is for in-game players only!");
                    return true;

                } else {

                    commandListBountiesPlayer(sender, sender.getName());
                    return true;

                }

            } else if (args[0].equalsIgnoreCase("recent")) {

                // list top bounties
                if (args.length == 1) {

                    commandListAllBounties(sender, 1);
                    return true;

                } else if (args.length == 2) {

                    Integer page;
                    try {
                        page = Integer.valueOf(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "[page #] in " + ChatColor.GREEN + "\"/bounty recent [page #]\"" + ChatColor.RED + " must be a number!");
                        return true;
                    }

                    commandListAllBounties(sender, page);
                    return true;

                } else {

                    sender.sendMessage(ChatColor.RED + "Too Many Arguments!");
                    return true;

                }

            } else if (args[0].equalsIgnoreCase("getheads") || args[0].equalsIgnoreCase("getskulls")) {

                // give player any skulls owed to them
                if (!(sender instanceof Player)) {

                    sender.sendMessage(ChatColor.RED + "This command is for in-game players only!");
                    return true;

                } else {

                    commandGetOwedHeads(sender);
                    return true;

                }

            } else if (args[0].equalsIgnoreCase("info")) {

                if (args.length != 2) {

                    sender.sendMessage(ChatColor.RED + "Use \"/bounty info [bounty #]\" for more details on a bounty");
                    return true;

                } else {

                    Integer page;
                    try {
                        page = Integer.valueOf(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "[bounty #] in " + ChatColor.GREEN + "\"/bounty info [bounty #]\"" + ChatColor.RED + " must be a number!");
                        return true;
                    }

                    commandBountyInfo(sender, page);
                    return true;

                }

            } else if (args[0].equalsIgnoreCase("plugin")) {

                sender.sendMessage(ChatColor.AQUA + "Plugin Name: " + this.getDescription().getName());
                sender.sendMessage(ChatColor.AQUA + "Author: " + this.getDescription().getAuthors());
                sender.sendMessage(ChatColor.AQUA + "Version: " + this.getDescription().getVersion());

                return true;

            } else {

                sender.sendMessage(ChatColor.RED + "Improper Usage!");
                Misc.sendHalp(sender);
                return true;

            }

        }

        return false;

    }

    public void commandAddBounty(final CommandSender sender, final String targetPlayerString) {

        if (this.config.useWhitelist) {
            final Player issuer = (Player) sender;
            ItemStack testItem = issuer.getItemInHand();

            boolean onWhitelist = false;
            for (String testMaterial : this.config.bountyMaterialsWhitelist) {
                if (testItem.getType().equals(Material.getMaterial(testMaterial))) onWhitelist = true;
            }

            if (!onWhitelist) {
                issuer.sendMessage(ChatColor.RED + "You can not use that item for bounties!");
                System.out.println(ChatColor.RED + issuer.getName() + " tried to use a " + testItem.getType().toString() + " as a bounty, but it was not on the whitelist!");
                return;
            }
        }

        Bukkit.getServer().getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                // This is async, because getOfflinePlayer(string) is blocking.
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerString);

                if (!targetPlayer.isOnline() && !targetPlayer.hasPlayedBefore()) {
                    sender.sendMessage(ChatColor.RED + "No such player " + targetPlayerString + "!");
                    return;
                }

                final Player issuer = (Player) sender;

                if (issuer.equals(targetPlayer)) {
                    sender.sendMessage(ChatColor.RED + "You may not place a bounty on yourself!");
                    return;
                }

                ItemStack item = issuer.getItemInHand();

                if (item.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "Hold an item in your hand to use it as a bounty");
                    return;
                } else if (item.getType() == Material.SKULL_ITEM) {
                    sender.sendMessage(ChatColor.RED + "You can't use a skull as a skull bounty, sorry.  (Ey Dawg)");
                    return;
                }

                //todo: check item against a whitelist

                sender.sendMessage(ChatColor.RED + "About to add " + ChatColor.GREEN + String.valueOf(item.getAmount()) + " " + item.getType().toString()
                        + ChatColor.RED + " to " + ChatColor.GREEN + targetPlayer.getName() + "'s " + ChatColor.RED + "bounty!");
                sender.sendMessage(ChatColor.RED + "Type " + ChatColor.GREEN + "/bounty confirm" + ChatColor.RED + " to confirm!");
                _pendingBounties.put(issuer.getUniqueId(), new AbstractMap.SimpleEntry<UUID, ItemStack>(targetPlayer.getUniqueId(), item));

                Bukkit.getServer().getScheduler().runTaskLater(_plugin, new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (_pendingBounties.containsKey(issuer.getUniqueId())) {
                            _pendingBounties.remove(issuer.getUniqueId());
                        }
                    }
                }, 400L);
            }
        });

    }

    public void commandConfirmAddBounty(CommandSender sender) {

        Player player = (Player) sender;

        if (!_pendingBounties.containsKey(player.getUniqueId())) {

            sender.sendMessage(ChatColor.RED + "You have not used " + ChatColor.GREEN +"/bounty add [PlayerName] " + ChatColor.RED + "in the last 20 seconds!");
            return;

        }

        Map.Entry<UUID, ItemStack> bounty = _pendingBounties.get(player.getUniqueId());
        _pendingBounties.remove(player.getUniqueId());
        final UUID targetUUID = bounty.getKey();
        final UUID issuerUUID = player.getUniqueId();
        ItemStack bountyItem = bounty.getValue();
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);
        final String targetPlayerName = targetPlayer.getName();
        final String issuerName = player.getName();

        if (!player.getInventory().contains(bountyItem)) {

            player.sendMessage(ChatColor.RED + "Your inventory no longer contains the bounty item!  Try again fool!");
            return;

        }

        //todo: am I missing anything?  Item type, damage, custom name, enchantments
        //todo: I supposed we should save lore too, but who cares
        // The following section parses all the item info for database add
        final String material = bountyItem.getType().toString();
        final int count = bountyItem.getAmount();
        HashMap<String, Integer> enchantments = new HashMap<String, Integer>();
        if (bountyItem.getEnchantments() != null) {
            Map<Enchantment, Integer> enchantsMap = bountyItem.getEnchantments();
            for (Map.Entry<Enchantment, Integer> entry: enchantsMap.entrySet()) {
                enchantments.put(entry.getKey().getName(), entry.getValue());
            }
        } else {
            enchantments = null;
        }
        final HashMap<String, Integer> finalEnchants = enchantments;
        final short durability = bountyItem.getDurability();
        String name = null;
        if (bountyItem.getItemMeta().hasDisplayName()) {
            name = bountyItem.getItemMeta().getDisplayName();
        }
        final String finalName = name;

        Bukkit.getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                int bountyID = _dao.addBounty(targetUUID, material, count, finalEnchants, durability, finalName,  issuerUUID);
                System.out.println("[NerdBounty] " + issuerName + " has placed bountyID " + bountyID + " on " + targetPlayerName);
            }
        });

        player.sendMessage(ChatColor.RED + "You have added " + ChatColor.GREEN + String.valueOf(bountyItem.getAmount()) + " " + bountyItem.getType().toString()
                + ChatColor.RED + " to " + ChatColor.GREEN + targetPlayerName + "'s " + ChatColor.RED + "bounty!");

        if (player.getItemInHand().equals(bountyItem)){
            player.getInventory().clear(player.getInventory().getHeldItemSlot());
        } else {
            player.getInventory().clear(player.getInventory().first(bountyItem));
        }

        if (_bountyAnnounceCooldowns.containsKey(player.getUniqueId()) &&
                (_bountyAnnounceCooldowns.get(player.getUniqueId()) > System.currentTimeMillis() - this.config.secondsBountyBroadcastCooldown*1000L)) {
            _bountyAnnounceCooldowns.remove(player.getUniqueId());
            _bountyAnnounceCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage("Your bounty was not announced (antispam)");
            return;
        } else if (_bountyAnnounceCooldowns.containsKey(player.getUniqueId())) {
            _bountyAnnounceCooldowns.remove(player.getUniqueId());
        }

        _bountyAnnounceCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        Bukkit.getServer().broadcastMessage(ChatColor.AQUA + player.getName()
                + ChatColor.GREEN
                + " has placed a new bounty on "
                + ChatColor.AQUA
                + targetPlayer.getName()
                + ChatColor.GREEN
                + "!");
        Bukkit.getServer().broadcastMessage(ChatColor.DARK_GRAY + "Use \"/bounty recent\" to see all recently placed bounties");

    }

    public void commandListBountiesSkull(CommandSender sender) {

        final Player player = (Player) sender;
        ItemStack skull = player.getItemInHand();

        if (!(player.getItemInHand().getType() == Material.SKULL_ITEM) ||
                !((SkullMeta) player.getItemInHand().getItemMeta()).hasOwner()) {

            sender.sendMessage(ChatColor.RED + "Please hold a head in your hand to use this command!");
            return;

        }

        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (!skullMeta.hasLore()) {
            sender.sendMessage(ChatColor.RED + "Could not redeem that skull.  It does not have the proper metadata.");
            return;
        }
        String skullIDString = skullMeta.getLore().get(1).substring(9);

        int testSkullID;
        try {
            testSkullID = Integer.parseInt(skullIDString);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Could not redeem that skull.  It does not have the proper metadata.");
            return;
        }
        final int skullID = testSkullID;

        final String skullOwnerString = skullMeta.getOwner();

        Bukkit.getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {

                UUID skullPlayerUUID = Bukkit.getOfflinePlayer(skullOwnerString).getUniqueId();

                if (_dao.skullHasBeenRedeemed(skullID)) {
                    player.sendMessage(ChatColor.RED + "That head has already been redeemed for a bounty in the past!");
                } else {
                    ArrayList<Integer> bounties = _dao.getBounties(skullPlayerUUID);
                    if (bounties == null) {
                        player.sendMessage(ChatColor.RED + "That head has no bounties!");
                        return;
                    }
                    player.sendMessage(ChatColor.RED + "-- That head has the following bounties --");
                    for (Integer i : bounties) {
                        String description = _dao.getBountyDescriptionCheckFormat(i);
                        player.sendMessage(description);
                    }
                    player.sendMessage(ChatColor.GRAY + "Type \"/bounty redeem [bounty #]\" to redeem the head for a bounty");
                }
            }
        });

    }

    public void commandBountyInfo(final CommandSender sender, final int bountyID) {

        Bukkit.getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {

                sender.sendMessage(_dao.getBountyDescriptionInfoFormat(bountyID));

            }
        });

    }

    public void commandRedeemSkullBounty(CommandSender sender, final int bountyID) {

        final Player player = (Player) sender;
        final ItemStack skull = player.getItemInHand();

        if (!(skull.getType() == Material.SKULL_ITEM) ||
                !((SkullMeta)skull.getItemMeta()).hasOwner()) {

            sender.sendMessage(ChatColor.RED + "Please hold a head in your hand to use this command!");
            return;

        }

        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (!skullMeta.hasLore()) {
            sender.sendMessage(ChatColor.RED + "Could not redeem that skull.  It does not have the proper metadata.");
            return;
        }
        String skullIDString = skullMeta.getLore().get(1).substring(9);

        int testSkullID;
        try {
            testSkullID = Integer.parseInt(skullIDString);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Could not redeem that skull.  It does not have the proper metadata.");
            return;
        }
        final int skullID = testSkullID;

        if (!skullMeta.hasLore() || !skullMeta.getLore().contains(ChatColor.AQUA + "Killed by " + player.getName())) {
            sender.sendMessage(ChatColor.RED + "You may only redeem heads from your own kills!");
            return;
        }

        final String skullOwnerString = skullMeta.getOwner();


        Bukkit.getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {

                UUID skullPlayerUUID = Bukkit.getOfflinePlayer(skullOwnerString).getUniqueId();

                if (!_dao.bountyExists(bountyID)) {
                    player.sendMessage(ChatColor.RED + "Invalid Bounty Number!");
                } else if (_dao.skullHasBeenRedeemed(skullID)) {
                    player.sendMessage(ChatColor.RED + "That head has already been redeemed for a bounty in the past!");
                } else {
                    UUID bountyTargetUUID = _dao.getBountyTarget(bountyID);
                    if (!bountyTargetUUID.equals(skullPlayerUUID) || _dao.bountyHasBeenRedeemed(bountyID)) {
                        player.sendMessage(ChatColor.RED + "That head cannot be redeemed for bounty " + ChatColor.GREEN + "#" + bountyID);
                        player.sendMessage(ChatColor.RED + "Type " + ChatColor.GREEN + "/bounty redeem" + ChatColor.RED + " to see all the available bounties for this head");
                    } else {

                        if (!player.getInventory().contains(skull)) {

                            player.sendMessage(ChatColor.RED + "Your inventory no longer contains the head that was about to be redeemed... try again?");

                        } else {

                            _dao.redeemSkull(skullID, _dao.getBountyIssuer(bountyID));
                            final ArrayList<ItemStack> bountyItems = _dao.redeemBounty(bountyID);
                            Bukkit.getServer().broadcastMessage(ChatColor.AQUA + player.getName()
                                    + ChatColor.GREEN
                                    + " has collected a bounty on "
                                    + ChatColor.AQUA
                                    + ((SkullMeta) skull.getItemMeta()).getOwner()
                                    + ChatColor.GREEN
                                    + "!");

                            UUID bountyIssuerUUID = _dao.getBountyIssuer(bountyID);
                            OfflinePlayer issuer = Bukkit.getOfflinePlayer(bountyIssuerUUID);
                            if (issuer.isOnline()) {
                                player.sendMessage(ChatColor.GREEN + player.getName() + ChatColor.RED + " redeemed your bounty!");
                                player.sendMessage(ChatColor.RED + "Use " + ChatColor.GREEN + "/bounty getheads " + ChatColor.RED + "to get your owed heads!");
                            }

                            // do inventory edits sync
                            Bukkit.getScheduler().runTask(_plugin, new BukkitRunnable() {
                                @Override
                                public void run() {

                                    // remove exactly one head
                                    if (skull.getAmount() > 1) skull.setAmount(1);
                                    player.getInventory().remove(skull);

                                    // add bounty items
                                    for (ItemStack bountyItem : bountyItems) {
                                        if (player.isOnline() && player.getInventory().firstEmpty() != -1) {

                                            player.getInventory().addItem(bountyItem);

                                        } else {

                                            player.getWorld().dropItemNaturally(player.getLocation(), bountyItem);

                                        }
                                    }
                                }
                            });

                        }

                    }
                }
            }
        });

    }

    public void commandListBountiesPlayer(CommandSender sender, final String playerName) {

        final CommandSender finalSender = sender;

        Bukkit.getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                ArrayList<Integer> bounties = _dao.getBounties(playerUUID);
                if (bounties == null) {

                    finalSender.sendMessage(ChatColor.RED + playerName + " has no outstanding bounties!");

                } else {
                    finalSender.sendMessage(ChatColor.RED + "-- " + playerName + " has the following bounties --");
                    for (Integer i : bounties) {
                        String description = _dao.getBountyDescriptionCheckFormat(i);
                        finalSender.sendMessage(description);
                    }
                    finalSender.sendMessage(ChatColor.GRAY + "Type \"/bounty redeem [bounty #]\" to redeem their head for a bounty");
                }
            }
        });

    }

    public void commandListAllBounties(CommandSender sender, final int page) {

        final CommandSender finalSender = sender;

        Bukkit.getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                ArrayList<Integer> bounties = _dao.getRecentBounties();

                if (bounties.isEmpty()) {

                    finalSender.sendMessage(ChatColor.RED + "There are no outstanding bounties!");

                } else {
                    ArrayList<String> constructedBounties = new ArrayList<String>();
                    for (Integer i : bounties) {
                        String description = _dao.getBountyDescriptionRecentFormat(i);
                        if (!constructedBounties.contains(description)){
                            constructedBounties.add(description);  //prevents duplicates
                        }
                    }
                    int totalPages = constructedBounties.size()/3 + 1;
                    int currentPage = page;
                    if (page > totalPages) currentPage = totalPages;
                    finalSender.sendMessage(ChatColor.RED + "-- Recent Bounties (Page " + currentPage + "/" + totalPages +") --");
                    for (int i = (currentPage-1)*3; (i < ((currentPage-1)*3) + 3) && i < constructedBounties.size(); i++) {
                        finalSender.sendMessage(constructedBounties.get(i));
                    }
                    if (currentPage < totalPages) finalSender.sendMessage(ChatColor.GRAY + "Type \"/bounty recent [page #]\" to see more");
                }
            }
        });

    }

    public void commandGetOwedHeads(CommandSender sender) {
        //prereq: sender instanceof player
        final Player player = (Player) sender;

        Bukkit.getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                ArrayList<Integer> owedSkullIDs = _dao.getOwedSkulls(player.getUniqueId());
                if (owedSkullIDs == null) {
                    player.sendMessage(ChatColor.RED + "You are not owed any skulls!");
                } else {
                    for (int skullID : owedSkullIDs) {
                        final int finalSkullID = skullID;
                        UUID victimUUID = _dao.getSkullVictim(skullID);
                        UUID killerUUID = _dao.getSkullKiller(skullID);
                        // assuming players with skulls on the server have joined before
                        final String victimName = Bukkit.getOfflinePlayer(victimUUID).getName();
                        final String killerName = Bukkit.getOfflinePlayer(killerUUID).getName();
                        _dao.setSkullCollected(skullID);

                        Bukkit.getScheduler().runTask(_plugin, new BukkitRunnable() {
                            @Override
                            public void run() {
                                ItemStack playerSkull = Misc.createSkull(victimName, killerName, finalSkullID);
                                if (player.isOnline() && player.getInventory().firstEmpty() != -1) {

                                    player.getInventory().addItem(playerSkull);

                                } else {

                                    player.getWorld().dropItemNaturally(player.getLocation(), playerSkull);

                                }
                            }
                        });

                    }
                }
            }
        });

    }

    public boolean hasBounty(Player victim) {

        return _dao.hasBounty(victim.getUniqueId());

    }

    public int createNewSkullEntry(Player victim, Player killer) {

        return _dao.createNewSkullEntry(victim.getUniqueId(), killer.getUniqueId());

    }

    public String getSkullKiller(int skullID) {

        UUID skullKiller = _dao.getSkullKiller(skullID);

        if (skullKiller != null)
            return Bukkit.getOfflinePlayer(skullKiller).getName();
        else
            return null;

    }

    public int getSkullAtLocation(Location skullLocation) {

        return _dao.getSkullID(skullLocation);

    }

    public void setSkullAsBroken(int skullID) {

        _dao.setSkullAsBroken(skullID);

    }

    public void setSkullAsPlaced(int skullID, Location location) {

        _dao.setSkullAsPlaced(skullID, location);

    }

    public boolean playerIsOwedHeads(UUID playerUUID) {

        return (_dao.getOwedSkulls(playerUUID) != null);

    }


}
