package com.gmail.fathersouth;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;

public class Config {

    private NerdBounty _plugin;
    private FileConfiguration _config;

    public Config(NerdBounty caller) {

        _plugin = caller;
        _config = caller.getConfig();

        //useMySQL = _config.getBoolean("useMySQL", false);  //this is commented out, because useMySQL = true is not implemented
        secondsAfterSameKillerNoHeadDrop = _config.getInt("secondsAfterSameKillerNoHeadDrop", 1800);
        secondsAfterDeathNoHeadDrop = _config.getInt("secondsAfterDeathNoHeadDrop", 30);
        alwaysDropHeadsForArmorKills = _config.getBoolean("alwaysDropHeadsForArmorKills", true);
        secondsBountyBroadcastCooldown = _config.getInt("secondsBountyBroadcastCooldown", 60);
        useWhitelist = _config.getBoolean("useWhitelist", true);
        bountyMaterialsWhitelist = (ArrayList<String>)_config.getList("bountyMaterialsWhitelist");

    }

    // MySQL not implemented yet.  Defaults to SQLite, in the plugin directory.  Keep this false.
    public boolean useMySQL = false;

    // After someone is beheaded, they cannot be beheaded again by the same player for
    // "secondsAfterSameKillerNoHeadDrop" seconds.  (Default 1800sec = 30 min)
    // This stops people from NOT redeeming a bounty to instead farm a re-spawning player for heads.
    // (since someone with a bounty will drop a head even when unarmed)
    public int secondsAfterSameKillerNoHeadDrop = 1800;

    // After someone is killed by ANYONE, they cannot be beheaded again for
    // "secondsAfterDeathNoHeadDrop" seconds.  This stops people from doing the same as above,
    // but using multiple killers to farm heads.  (Default 30sec)
    // This is short by default, because it applies to EVERYONE who kills the same player, and
    // we don't want to make someone completely invulnerable to beheadings for very long.
    public int secondsAfterDeathNoHeadDrop = 30;

    // Armor kills (Prot 3+ Chestplate/Leggings kills) can ignore the no-beheading cooldown, on
    // the assumption that if someone re-armors, they're probably not being spawn-killed.
    public boolean alwaysDropHeadsForArmorKills = true;

    // This is a cooldown on the "Player1 added a bounty on Player2" announcements, to prevent
    // chat spam.
    public int secondsBountyBroadcastCooldown = 60;

    public boolean useWhitelist = true;

    public ArrayList<String> bountyMaterialsWhitelist;

}
