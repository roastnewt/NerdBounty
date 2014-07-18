package com.gmail.fathersouth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//todo: adjust queries so they'll work with mysql as well as sqlite syntax

public class NerdBountyDAO
{

    private Connection _sql;
    private NerdBounty plugin;

    NerdBountyDAO(NerdBounty callingPlugin) {

        plugin = callingPlugin;

    }

    public void connect( String hostname, String database, String username, String password, File dir )
    {
        if (this.plugin.config.useMySQL)
        {
            System.out.println("[NerdBounties] Using MySQL!");
            try
            {
                Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
                this._sql = DriverManager.getConnection( "jdbc:mysql://" + hostname + "/" + database, username, password );
            } catch( Exception e ) {
                System.err.println( "[NerdBounties] Cannot connect to database server: " + e.getMessage() );
                return;
            }

            try {
                DatabaseMetaData dbm = this._sql.getMetaData();
                ResultSet tables = dbm.getTables(null, null, "bounties", null);
                if (!tables.next()){
                    //table does not exist
                    System.out.println("[NerdBounties] No bounties table found!  Creating...");
                    createBountiesTable();
                }
            } catch( Exception e) {
                System.err.println( "[NerdBounties] Cannot get db metadata: " + e.getMessage() );
                return;
            }

            try {
                DatabaseMetaData dbm = this._sql.getMetaData();
                ResultSet tables = dbm.getTables(null, null, "heads", null);
                if (!tables.next()){
                    //table does not exist
                    System.out.println("[NerdBounties] No heads table found!  Creating...");
                    createHeadsTable();
                }
            } catch( Exception e) {
                System.err.println( "[NerdBounties] Cannot get db metadata: " + e.getMessage() );
            }

            try {
                DatabaseMetaData dbm = this._sql.getMetaData();
                ResultSet tables = dbm.getTables(null, null, "enchantments", null);
                if (!tables.next()){
                    //table does not exist
                    System.out.println("[NerdBounties] No enchantments table found!  Creating...");
                    createEnchantmentsTable();
                }
            } catch( Exception e) {
                System.err.println( "[NerdBounties] Cannot get db metadata: " + e.getMessage() );
            }
        }
        else
        {
            System.out.println("[NerdBounties] No MySQL?  Using sqlite!");
            try
            {
                Class.forName( "org.sqlite.JDBC" ).newInstance();
                this._sql = DriverManager.getConnection( "jdbc:sqlite:" + dir +"/bounties.db");
            } catch( Exception e ) {
                System.err.println( "[NerdBounties] Cannot connect to database server: " + e.getClass().getName() + " : " + e.getMessage() );
                return;
            }

            try {
                DatabaseMetaData dbm = this._sql.getMetaData();
                ResultSet tables = dbm.getTables(null, null, "bounties", null);
                if (!tables.next()){
                    //table does not exist
                    System.out.println("[NerdBounties] No bounties table found!  Creating...");
                    createBountiesTable();
                }
            } catch( Exception e) {
                System.err.println( "[NerdBounties] Cannot get db metadata: " + e.getMessage() );
                return;
            }

            try {
                DatabaseMetaData dbm = this._sql.getMetaData();
                ResultSet tables = dbm.getTables(null, null, "heads", null);
                if (!tables.next()){
                    //table does not exist
                    System.out.println("[NerdBounties] No heads table found!  Creating...");
                    createHeadsTable();
                }

            } catch( Exception e) {
                System.err.println( "[NerdBounties] Cannot get db metadata: " + e.getMessage() );
            }

            try {
                DatabaseMetaData dbm = this._sql.getMetaData();
                ResultSet tables = dbm.getTables(null, null, "enchantments", null);
                if (!tables.next()){
                    //table does not exist
                    System.out.println("[NerdBounties] No enchantments table found!  Creating...");
                    createEnchantmentsTable();
                }
            } catch( Exception e) {
                System.err.println( "[NerdBounties] Cannot get db metadata: " + e.getMessage() );
            }
        }

    }

    public void createBountiesTable() {
        try {

            if (this.plugin.config.useMySQL) {
                PreparedStatement s = this._sql.prepareStatement("CREATE TABLE bounties " +
                        "(bountyID int(50) not null auto_increment primary key, " +
                        "targetUUID varchar(35), " +
                        "issuerUUID varchar(35), " +
                        "redeemed boolean, " +
                        "material varchar(35), " +
                        "count int(35), " +
                        "durability int(10), " +
                        "itemName varchar(35)," +
                        "dateIssued datetime," +
                        "dateRedeemed datetime)");
                s.execute();
            } else {
                PreparedStatement s = this._sql.prepareStatement("CREATE TABLE bounties " +
                        "(bountyID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "targetUUID varchar(35), " +
                        "issuerUUID varchar(35), " +
                        "redeemed boolean, " +
                        "material varchar(35), " +
                        "count int(35), " +
                        "durability SHORT INTEGER, " +
                        "itemName varchar(35)," +
                        "dateIssued datetime," +
                        "dateRedeemed datetime)");
                s.execute();
            }

        }
        catch( Exception e ) {
            System.err.println("Exception creating table: " + e.getMessage());
        }
    }

    public void createHeadsTable() {
        try {
            // REDEEMED means that someone redeemed a head to get the bounty
            // COLLECTED means the person that placed the bounty has collected their reward head. (giggity)
            // bountyIssuerUUID is null, unless the skull has been redeemed for a bounty
            if (this.plugin.config.useMySQL) {
                PreparedStatement s = this._sql.prepareStatement("CREATE TABLE heads " +
                        "(headID int(50) not null auto_increment primary key, " +
                        "victimUUID varchar(35), " +
                        "killerUUID varchar(35), " +
                        "bountyIssuerUUID varchar(35), " +
                        "redeemed boolean, " +
                        "collected boolean, " +
                        "isPlaced boolean, " +
                        "world varchar(35), " +
                        "xcoord double(35), " +
                        "ycoord double(35), " +
                        "zcoord double(35))");
                s.execute();
            } else {
                PreparedStatement s = this._sql.prepareStatement("CREATE TABLE heads " +
                        "(headID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "victimUUID varchar(35), " +
                        "killerUUID varchar(35), " +
                        "bountyIssuerUUID varchar(35), " +
                        "redeemed boolean, " +
                        "collected boolean, " +
                        "isPlaced boolean, " +
                        "world varchar(35), " +
                        "xcoord REAL, " +
                        "ycoord REAL, " +
                        "zcoord REAL)");
                s.execute();
            }
        }
        catch( Exception e ) {
            System.err.println("Exception creating table: " + e.getMessage());
        }
    }

    public void createEnchantmentsTable() {

        try {
            if (this.plugin.config.useMySQL) {
                PreparedStatement s = this._sql.prepareStatement("CREATE TABLE enchantments " +
                        "(enchantID int(50) not null auto_increment primary key, " +
                        "bountyID int(50), " +
                        "enchantName varchar(35), " +
                        "enchantLevel int(10))");
                s.execute();
            } else {
                PreparedStatement s = this._sql.prepareStatement("CREATE TABLE enchantments " +
                        "(enchantID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "bountyID int(50), " +
                        "enchantName varchar(35), " +
                        "enchantLevel int(10))");
                s.execute();
            }
        }
        catch( Exception e ) {
            System.err.println("Exception creating table: " + e.getMessage());
        }

    }

    public void disconnect() {
        if ( this._sql == null ) return; // Ignore unopened connections
        try {
            this._sql.close();
        } catch( Exception e ) {
            System.err.println( "Cannot close database connection: " + e.getMessage() );
        }
    }

    public void keepAlive(){

        try {
            PreparedStatement s = this._sql.prepareStatement("SELECT count(*) FROM bounties WHERE dateIssued = 1");
            s.executeQuery();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public boolean hasBounty(UUID playerUUID) {

        return (getBounties(playerUUID) != null);

    }

    public int addBounty(UUID playerUUID, String material, int count, HashMap<String, Integer> enchantments, short durability, String name,  UUID issuerUUID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("INSERT INTO bounties (targetUUID, issuerUUID, redeemed, material, count, durability, itemName, dateIssued) VALUES(?, ?, 0, ?, ?, ?, ?, ?)");
            s.setString(1, playerUUID.toString());
            s.setString(2, issuerUUID.toString());
            s.setString(3, material);
            s.setInt(4, count);
            s.setShort(5, durability);
            s.setString(6, name);
            s.setDate(7, new Date(System.currentTimeMillis()));

            s.executeUpdate();
            ResultSet rs = s.getGeneratedKeys();
            int bountyID = -1;
            if (rs.next()) {
                 bountyID = rs.getInt(1);
            }

            if (enchantments != null && bountyID != -1) {

                for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {

                    PreparedStatement s2 = this._sql.prepareStatement("INSERT INTO enchantments (bountyID, enchantName, enchantLevel) VALUES(?, ?, ?)");
                    s2.setInt(1, bountyID);
                    s2.setString(2, entry.getKey());
                    s2.setInt(3, entry.getValue());

                    s2.executeUpdate();

                }

            }

            return bountyID;

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return -1;


    }

    public boolean bountyHasBeenRedeemed(int bountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT redeemed FROM bounties WHERE bountyID = ?");
            s.setInt(1, bountyID);
            ResultSet rs = s.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return true;

    }

    public ArrayList<ItemStack> redeemBounty(int bountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT targetUUID, issuerUUID FROM bounties WHERE bountyID = ? AND redeemed = 0");
            s.setInt(1, bountyID);

            ResultSet rs2 = s.executeQuery();

            if (rs2.next()) {
                String targetUUIDString = rs2.getString(1);
                String issuerUUIDString = rs2.getString(2);

                PreparedStatement s2 = this._sql.prepareStatement("SELECT bountyID, material, count, durability, itemName FROM bounties WHERE issuerUUID = ? AND targetUUID = ? AND redeemed = 0");
                s2.setString(1, issuerUUIDString);
                s2.setString(2, targetUUIDString);

                ResultSet rs = s2.executeQuery();

                ArrayList<ItemStack> constructedItemList = new ArrayList<ItemStack>();

                if (rs.next()) {
                    do {
                        int bountyID2 = rs.getInt(1);
                        String material = rs.getString(2);
                        int count = rs.getInt(3);
                        short durability = rs.getShort(4);
                        String itemName = rs.getString(5);

                        ItemStack constructedItem = new ItemStack(Material.getMaterial(material), count);
                        if (durability != -1) constructedItem.setDurability(durability);
                        if (itemName != null) {
                            ItemMeta meta = constructedItem.getItemMeta();
                            meta.setDisplayName(itemName);
                            constructedItem.setItemMeta(meta);
                        }

                        PreparedStatement s4 = this._sql.prepareStatement("SELECT enchantName, enchantLevel FROM enchantments WHERE bountyID = ?");
                        s4.setInt(1, bountyID2);
                        ResultSet rs3 = s4.executeQuery();

                        ItemMeta meta = constructedItem.getItemMeta();
                        while (rs3.next()) {
                            meta.addEnchant(Enchantment.getByName(rs3.getString(1)), rs3.getInt(2), true);
                        }
                        constructedItem.setItemMeta(meta);

                        PreparedStatement s3 = this._sql.prepareStatement("UPDATE bounties SET redeemed = 1, dateRedeemed = ? WHERE bountyID = ?");
                        s3.setDate(1, new Date(System.currentTimeMillis()));
                        s3.setInt(2, bountyID2);

                        s3.executeUpdate();
                        System.out.println("[NerdBounty] bountyID " + bountyID2 + " has been redeemed for " + count + " " + material);

                        constructedItemList.add(constructedItem);

                    } while (rs.next());

                    return constructedItemList;

                }

            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;

    }

    public ArrayList<Integer> getBounties(UUID playerUUID) {

        try {

            //only list first bountyID from each issuer
            PreparedStatement s = this._sql.prepareStatement("SELECT bountyID FROM bounties WHERE targetUUID = ? AND redeemed = 0 GROUP BY issuerUUID");
            s.setString(1, playerUUID.toString());

            ResultSet rs = s.executeQuery();

            ArrayList<Integer> constructedBountyList = new ArrayList<Integer>();
            if (!rs.next()) {
                return null;
            } else {
                do {
                    constructedBountyList.add(rs.getInt(1));
                } while(rs.next());
            }

            return constructedBountyList;

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;

    }

    public String getBountyDescriptionInfoFormat(int BountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT targetUUID, issuerUUID FROM bounties WHERE bountyID = ?");
            s.setInt(1, BountyID);
            ResultSet rs = s.executeQuery();
            if (!rs.next()) return null;
            String targetUUIDString = rs.getString(1);
            String issuerUUIDString = rs.getString(2);

            PreparedStatement s2 = this._sql.prepareStatement("SELECT bountyID, material, sum(count), durability, itemName, dateIssued FROM bounties WHERE targetUUID = ? AND issuerUUID = ? AND redeemed = 0 GROUP BY material, durability");
            s2.setString(1, targetUUIDString);
            s2.setString(2, issuerUUIDString);

            rs = s2.executeQuery();

            // "/bounty recent" depends on this structure.  If you have to change it, please check in NerdBounty.java first
            if (rs.next()) {

                String constructedString = ChatColor.AQUA + "#" + rs.getInt(1) + ": "
                        + ChatColor.RED
                        + "Bounty on "
                        + ChatColor.AQUA
                        + Bukkit.getOfflinePlayer(UUID.fromString(targetUUIDString)).getName() + ":";

                do {
                    constructedString = constructedString + ChatColor.RED + "\n      Reward: " + ChatColor.GREEN + rs.getInt(3) + " " + rs.getString(2);

                    if (rs.getShort(4) != (short)0) {
                        int maxDurability = Material.getMaterial(rs.getString(2)).getMaxDurability();
                        int damage = rs.getShort(4);
                        if (maxDurability != 0) damage = maxDurability - damage;  //this is a little fix so we can show the durability LEFT for things like armor
                        constructedString = constructedString + ChatColor.DARK_RED + "\n        " + "Durability: " + ChatColor.YELLOW + damage + "/" + maxDurability;
                    }
                    if (rs.getString(5) != null) {
                        constructedString = constructedString + ChatColor.DARK_RED + "\n        " + "Name: " + ChatColor.YELLOW + rs.getString(5);
                    }

                    PreparedStatement s3 = this._sql.prepareStatement("SELECT enchantName, enchantLevel FROM enchantments WHERE bountyID = ?");
                    s3.setInt(1, rs.getInt(1));
                    ResultSet rs3 = s3.executeQuery();
                    while (rs3.next()) {
                        constructedString = constructedString + ChatColor.DARK_RED + "\n        " + "Enchantment: " + ChatColor.YELLOW + rs3.getString(1) + " " + rs3.getInt(2);
                    }

                    constructedString = constructedString + "\n";

                } while (rs.next());

                constructedString = constructedString + "\n      Placed by: " + ChatColor.GREEN + Bukkit.getOfflinePlayer(UUID.fromString(issuerUUIDString)).getName();
                constructedString = constructedString + ChatColor.RED + "\n-----------------------------";
                return constructedString;

            } else {
                return "Invalid BountyID";
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;

    }

    public String getBountyDescriptionCheckFormat(int BountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT targetUUID, issuerUUID FROM bounties WHERE bountyID = ?");
            s.setInt(1, BountyID);
            ResultSet rs = s.executeQuery();
            if (!rs.next()) return null;
            String targetUUIDString = rs.getString(1);
            String issuerUUIDString = rs.getString(2);

            PreparedStatement s2 = this._sql.prepareStatement("SELECT bountyID, material, sum(count), dateIssued FROM bounties WHERE targetUUID = ? AND issuerUUID = ? AND redeemed = 0 GROUP BY material");
            s2.setString(1, targetUUIDString);
            s2.setString(2, issuerUUIDString);

            rs = s2.executeQuery();

            // /bounty recent depends on this structure.  If you have to change it, please check in NerdBounty.java first
            if (rs.next()) {
                String constructedString = ChatColor.AQUA + "#" + rs.getInt(1) + ": ";

                do {
                    constructedString = constructedString + ChatColor.GREEN + rs.getInt(3) + " " + rs.getString(2) + ChatColor.RED + ", ";
                } while (rs.next());

                constructedString = constructedString + "placed on " + ChatColor.GREEN + Bukkit.getOfflinePlayer(UUID.fromString(targetUUIDString)).getName()
                        + ChatColor.RED + " by " + ChatColor.GREEN + Bukkit.getOfflinePlayer(UUID.fromString(issuerUUIDString)).getName();
                return constructedString;
            } else {
                return "Invalid BountyID";
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;

    }

    public String getBountyDescriptionRecentFormat(int BountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT targetUUID, issuerUUID FROM bounties WHERE bountyID = ?");
            s.setInt(1, BountyID);
            ResultSet rs = s.executeQuery();
            if (!rs.next()) return null;
            String targetUUIDString = rs.getString(1);
            String issuerUUIDString = rs.getString(2);

            PreparedStatement s2 = this._sql.prepareStatement("SELECT bountyID, material, sum(count), dateIssued FROM bounties WHERE targetUUID = ? AND issuerUUID = ? AND redeemed = 0 GROUP BY material");
            s2.setString(1, targetUUIDString);
            s2.setString(2, issuerUUIDString);

            rs = s2.executeQuery();

            // "/bounty recent" depends on this structure.  If you have to change it, please check in NerdBounty.java first
            if (rs.next()) {

                String constructedString = ChatColor.AQUA + "#" + rs.getInt(1) + ": "
                        + ChatColor.RED
                        + "Bounty on "
                        + ChatColor.AQUA
                        + Bukkit.getOfflinePlayer(UUID.fromString(targetUUIDString)).getName() + ":" + ChatColor.RED + "\n      Reward: ";

                do {
                    constructedString = constructedString + ChatColor.YELLOW + rs.getInt(3) + " " + rs.getString(2) + ChatColor.RED + ", ";
                } while (rs.next());

                constructedString = constructedString + "\n      Placed by: " + ChatColor.GREEN + Bukkit.getOfflinePlayer(UUID.fromString(issuerUUIDString)).getName();
                constructedString = constructedString + ChatColor.RED + "\n-----------------------------";
                return constructedString;

            } else {
                return "Invalid BountyID";
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;

    }

    public UUID getBountyIssuer(int bountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT issuerUUID FROM bounties WHERE bountyID = ?");
            s.setInt(1, bountyID);

            ResultSet rs = s.executeQuery();

            if (rs.next())
                return UUID.fromString(rs.getString(1));
            else
                return null;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public UUID getBountyTarget(int bountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT targetUUID FROM bounties WHERE bountyID = ?");
            s.setInt(1, bountyID);

            ResultSet rs = s.executeQuery();

            if (rs.next())
                return UUID.fromString(rs.getString(1));
            else
                return null;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public ArrayList<Integer> getRecentBounties() {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT bountyID FROM bounties WHERE redeemed = 0 ORDER BY bountyID DESC");

            ResultSet rs = s.executeQuery();

            ArrayList<Integer> constructedList = new ArrayList<Integer>();


            while (rs.next()) {
                constructedList.add(rs.getInt(1));
            }

            return constructedList;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;


    }

    public boolean bountyExists(int bountyID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT bountyID FROM bounties WHERE bountyID = ?");
            s.setInt(1, bountyID);

            ResultSet rs = s.executeQuery();

            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;

    }

    //
    // Player Head Table Accessors
    //

    public ArrayList<Integer> getOwedSkulls(UUID issuerUUID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT headID FROM heads WHERE bountyIssuerUUID = ? AND collected = 0");
            s.setString(1, issuerUUID.toString());

            ResultSet rs = s.executeQuery();

            if (!rs.next()) {
                return null;
            } else {
                ArrayList<Integer> skulls = new ArrayList<Integer>();
                do {

                    skulls.add(rs.getInt(1));

                } while (rs.next());

                return skulls;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public void redeemSkull(int skullID, UUID issuerUUID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("UPDATE heads SET redeemed = 1, bountyIssuerUUID = ? WHERE headID = ?");
            s.setString(1, issuerUUID.toString());
            s.setInt(2, skullID);

            System.out.println("[NerdBounty] " + Bukkit.getOfflinePlayer(issuerUUID).getName() + " has redeemed headID#" + skullID + " for its bounty!");

            s.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setSkullCollected(int skullID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("UPDATE heads SET collected = 1 WHERE headID = ?");
            s.setInt(1, skullID);

            s.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int createNewSkullEntry(UUID victimUUID, UUID killerUUID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("INSERT INTO heads (victimUUID, killerUUID, redeemed, collected) VALUES(?, ?, 0, 0)");
            s.setString(1, victimUUID.toString());
            s.setString(2, killerUUID.toString());

            s.executeUpdate();
            ResultSet rs = s.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return -1;

    }

    public boolean skullHasBeenRedeemed(int headID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT redeemed FROM heads WHERE headID = ?");
            s.setInt(1, headID);
            ResultSet rs = s.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return true;

    }

    public UUID getSkullKiller(int skullID) {
        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT killerUUID FROM heads WHERE headID = ?");
            s.setInt(1, skullID);
            ResultSet rs = s.executeQuery();

            if (!rs.next())
            {
                return null;

            } else
            {
                return UUID.fromString(rs.getString(1));
            }

        } catch ( Exception e) {

            e.printStackTrace();

        }

        return null;

    }

    public UUID getSkullVictim(int skullID) {
        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT victimUUID FROM heads WHERE headID = ?");
            s.setInt(1, skullID);
            ResultSet rs = s.executeQuery();

            if (!rs.next())
            {
                return null;

            } else
            {
                return UUID.fromString(rs.getString(1));
            }

        } catch ( Exception e) {

            e.printStackTrace();

        }

        return null;

    }

    public int getSkullID(Location skullLocation) {

        try {

            PreparedStatement s = this._sql.prepareStatement("SELECT headID FROM heads WHERE isPlaced = 1 AND world = ? AND xcoord = ? AND ycoord = ? AND zcoord = ?");
            s.setString(1, skullLocation.getWorld().toString());
            s.setDouble(2, skullLocation.getX());
            s.setDouble(3, skullLocation.getY());
            s.setDouble(4, skullLocation.getZ());
            ResultSet rs = s.executeQuery();

            if (!rs.next())
            {
                return -1;

            } else
            {
                return rs.getInt(1);
            }

        } catch ( Exception e) {

            e.printStackTrace();

        }

        return -1;

    }

    public void setSkullAsBroken(int skullID) {

        try {

            PreparedStatement s = this._sql.prepareStatement("UPDATE heads SET isPlaced = 0 WHERE headID = ?");
            s.setInt(1, skullID);
            s.executeUpdate();

        } catch ( Exception e) {

            e.printStackTrace();

        }

    }

    public void setSkullAsPlaced(int skullID, Location skullLocation) {

        try {

            PreparedStatement s = this._sql.prepareStatement("UPDATE heads SET isPlaced = 1, world = ?, xcoord = ?, ycoord = ?, zcoord = ? WHERE headID = ?");
            s.setString(1, skullLocation.getWorld().toString());
            s.setDouble(2, skullLocation.getX());
            s.setDouble(3, skullLocation.getY());
            s.setDouble(4, skullLocation.getZ());
            s.setInt(5, skullID);
            s.executeUpdate();

        } catch ( Exception e) {

            e.printStackTrace();

        }


    }


}