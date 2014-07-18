package com.gmail.fathersouth;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    // Events:
    // PlayerDeath - give killer da head
    // Right-Click - tell whose head it is
    // Place Skull - record where it is
    // Break Skull - apply info

    private NerdBounty _plugin;

    private HashMap<UUID, Long> _playerDeathCooldowns = new HashMap<UUID, Long>();
    private HashMap<Map.Entry<UUID, UUID>, Long> _playerKillCooldowns = new HashMap<Map.Entry<UUID, UUID>, Long>();

    public PlayerListener(NerdBounty callingPlugin){

        _plugin = callingPlugin;

    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        if (event.getEntity().getKiller() != null && event.getEntity().getKiller() instanceof Player) {

            final Player victim = event.getEntity();
            final Location deathLocation = victim.getLocation();
            final Player killer = event.getEntity().getKiller();

            if (killer.equals(event.getEntity())) return;

            final boolean armorKill = shouldArmorHeadDrop(event);

            boolean tempViolatesCooldowns = false;

            if (_playerDeathCooldowns.containsKey(victim.getUniqueId())) {
                _playerDeathCooldowns.remove(victim.getUniqueId());
                _playerDeathCooldowns.put(victim.getUniqueId(), System.currentTimeMillis());

                if (_playerDeathCooldowns.get(victim.getUniqueId()) > (System.currentTimeMillis() - _plugin.config.secondsAfterDeathNoHeadDrop*1000L)) {
                    // basically, if it's been less than 30sec since their last death, don't drop a head.
                    // this reduces head-farming on people with bounties
                    if (!armorKill) killer.sendMessage("This player has already died in the last 30 seconds, so they did not drop a head");
                    tempViolatesCooldowns = true;

                }

            } else {
                _playerDeathCooldowns.put(victim.getUniqueId(), System.currentTimeMillis());
            }

            final AbstractMap.SimpleEntry<UUID, UUID> UUIDMap = new AbstractMap.SimpleEntry<UUID, UUID>(killer.getUniqueId(), victim.getUniqueId());
            if (_playerKillCooldowns.containsKey(UUIDMap)) {
                _playerKillCooldowns.remove(UUIDMap);
                _playerKillCooldowns.put(UUIDMap, System.currentTimeMillis());
                if (_playerKillCooldowns.get(UUIDMap) > (System.currentTimeMillis() - _plugin.config.secondsAfterSameKillerNoHeadDrop*1000L)) {
                    if (!tempViolatesCooldowns && !armorKill) killer.sendMessage("You have gotten a head from this player in the last half-hour, so they did not drop a head");
                    tempViolatesCooldowns = true;
                }
            } else {
                _playerKillCooldowns.put(UUIDMap, System.currentTimeMillis());
            }

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new BukkitRunnable() {
                @Override
                public void run() {
                    _playerDeathCooldowns.remove(victim.getUniqueId());
                }
            }, _plugin.config.secondsAfterDeathNoHeadDrop*20);

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new BukkitRunnable() {
                @Override
                public void run() {
                    _playerKillCooldowns.remove(UUIDMap);
                }
            }, _plugin.config.secondsAfterSameKillerNoHeadDrop*20);

            final boolean violatesCooldowns = tempViolatesCooldowns;

            //
            // Since looking up whether a player has a bounty involves a database call, we run hasBounty(victim)
            // Asynchronously, then dispatch a task back to the main thread to place the skull in the killer's
            // inventory.
            //

            Bukkit.getScheduler().runTaskAsynchronously(_plugin, new BukkitRunnable() {
                @Override
                public void run() {
                    if ((armorKill && (_plugin.config.alwaysDropHeadsForArmorKills || !violatesCooldowns)) || (_plugin.hasBounty(victim) && !violatesCooldowns)) {

                        final int skullID = _plugin.createNewSkullEntry(victim, killer);

                        Bukkit.getScheduler().runTask(_plugin, new BukkitRunnable() {
                            @Override
                            public void run() {

                                ItemStack playerSkull = Misc.createSkull(victim.getName(), killer.getName(), skullID);

                                Bukkit.getServer().broadcastMessage(ChatColor.RED + victim.getName() + " was beheaded by " + killer.getName());
                                // check that the killer is still online, DB lookup can take arbitrary time,
                                // they may have logged off.
                                if (killer.isOnline() && killer.getInventory().firstEmpty() != -1) {

                                    killer.getInventory().addItem(playerSkull);
                                    System.out.println("[NerdBounty] " + victim.getName() +"'s head was placed in " + killer.getName() + "'s inventory. (headID#" + skullID + ")");

                                } else {

                                    deathLocation.getWorld().dropItemNaturally(deathLocation, playerSkull);
                                    System.out.println("[NerdBounty] " + victim.getName() +"'s head dropped on the ground, because " + killer.getName() + "'s inventory was full. (headID#" + skullID + ")");

                                }
                            }
                        });

                    }
                }
            });

        }

    }

    public boolean shouldArmorHeadDrop (PlayerDeathEvent event) {
        // Prereqs:  ∃killer, is player

        Player killer = event.getEntity().getKiller();
        Player victim = event.getEntity();
        ItemStack victimChestplate = victim.getInventory().getChestplate();
        ItemStack victimPantaloons = victim.getInventory().getLeggings();
        ItemStack killerChestplate = killer.getInventory().getChestplate();
        ItemStack killerPantaloons = killer.getInventory().getLeggings();


        if (victimChestplate == null || victimPantaloons == null || killerChestplate == null || killerPantaloons == null) return false;

        Location deathLocation = victim.getLocation();

        Firework deathFirework = deathLocation.getWorld().spawn(deathLocation, Firework.class);
        FireworkMeta meta = deathFirework.getFireworkMeta();
        meta.addEffects(FireworkEffect.builder().withColor(Color.WHITE).withFade(Color.BLUE).with(FireworkEffect.Type.BALL_LARGE).build());
        deathFirework.setFireworkMeta(meta);

        if (victimChestplate.getType() != Material.IRON_CHESTPLATE
                && victimChestplate.getType() != Material.DIAMOND_CHESTPLATE) return false;
        if (victimPantaloons.getType() != Material.IRON_LEGGINGS
                && victimPantaloons.getType() != Material.DIAMOND_LEGGINGS) return false;
        if (victimChestplate.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 3) return false;
        if (victimPantaloons.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 3) return false;
        if (killerChestplate.getType() != Material.IRON_CHESTPLATE
                && killerChestplate.getType() != Material.DIAMOND_CHESTPLATE) return false;
        if (killerPantaloons.getType() != Material.IRON_LEGGINGS
                && killerPantaloons.getType() != Material.DIAMOND_LEGGINGS) return false;
        if (killerChestplate.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 3) return false;
        if (killerPantaloons.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 3) return false;

        System.out.println("[NerdBounty] Armor Requirements Met!  Head will drop...");
        deathLocation.getWorld().strikeLightningEffect(deathLocation);

        return true;

    }

    @EventHandler
    public void onPlayerClickSkull(PlayerInteractEvent event) {

        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock.getType() == Material.SKULL) {

                Skull skullState = (Skull)clickedBlock.getState();
                if (skullState.getSkullType() == SkullType.PLAYER && skullState.hasOwner()) {

                    String owner = skullState.getOwner();
                    event.getPlayer().playEffect(skullState.getLocation(), Effect.MOBSPAWNER_FLAMES, null);
                    event.getPlayer().sendMessage(ChatColor.AQUA + "That's " + ChatColor.RED + owner + "'s " + ChatColor.AQUA + "head!");

                }

            }

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerBreakSkull(BlockBreakEvent event) {

        if (!event.isCancelled() && event.getBlock().getType() == Material.SKULL) {

            // I guess I'm assuming a skull block only ever drops a single skull item and nothing else.
            // May conflict with other plugins- come back to this

            Block skullBlock = event.getBlock();
            final Location skullLocation = skullBlock.getLocation();
            ItemStack skullDrop = (ItemStack)event.getBlock().getDrops().toArray()[0];
            final String skullOwner = ((SkullMeta)skullDrop.getItemMeta()).getOwner();
            final String breakerPlayer = event.getPlayer().getName();

            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);

            //
            // These are two blocking methods - come back to this
            //
            Bukkit.getScheduler().runTaskAsynchronously(_plugin, new BukkitRunnable() {
                @Override
                public void run() {
                    final int skullID = _plugin.getSkullAtLocation(skullLocation);
                    final String killerName = _plugin.getSkullKiller(skullID);
                    _plugin.setSkullAsBroken(skullID);

                    Bukkit.getScheduler().runTask(_plugin, new BukkitRunnable() {
                        @Override
                        public void run() {
                            skullLocation.getWorld().dropItemNaturally(skullLocation, Misc.createSkull(skullOwner, killerName, skullID));

                            if (killerName == null) {
                                System.err.println("[NerdBounty] " + breakerPlayer + " has just broken an unregistered skull!");
                                System.err.println("[NerdBounty] It was either produced by another plugin, or there's been some sort of map rollback");
                                System.err.println("[NerdBounty] This skull will not be able to be used for bounties");
                            } else {
                                System.out.println("[NerdBounty] " + breakerPlayer + " just broke headID# " + skullID);
                            }
                        }
                    });
                }
            });

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPlaceSkull(BlockPlaceEvent event) {

        if (!event.isCancelled() && event.getBlockPlaced().getType() == Material.SKULL) {

            Block skullBlock = event.getBlockPlaced();
            ItemStack skullItem = event.getItemInHand();
            SkullMeta skullMeta = (SkullMeta)skullItem.getItemMeta();
            if (skullMeta.hasLore() && (((String)skullMeta.getLore().toArray()[1]).contains(ChatColor.DARK_GRAY + "Skull #"))) {

                final int skullID = Integer.valueOf(((String)skullMeta.getLore().toArray()[1]).substring(9));
                final Location skullLocation = skullBlock.getLocation();
                Bukkit.getScheduler().runTaskAsynchronously(_plugin, new BukkitRunnable() {
                    @Override
                    public void run() {
                        _plugin.setSkullAsPlaced(skullID, skullLocation);
                    }
                });

                System.out.println("[NerdBounty] " + event.getPlayer().getName() + " just broke headID# " + skullID);

            } else {
                System.err.println("[NerdBounty] " + event.getPlayer().getName() + " has just placed an unregistered skull!");
                System.err.println("[NerdBounty] It was either produced by another plugin, or there's been some sort of map rollback");
                System.err.println("[NerdBounty] This skull will not be able to be used for bounties");
            }

        }

    }

}
