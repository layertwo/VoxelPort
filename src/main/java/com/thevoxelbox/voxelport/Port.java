package com.thevoxelbox.voxelport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * @author Voxel
 */
public class Port
{

    private final String portalName;
    private Zone portalZone;
    private Location arrivalLocation;
    private Location departLocation;
    private Location redstoneKey;
    private final TreeSet<Integer> departures = new TreeSet<Integer>();
    private final ArrayList<String> welcomeMessages = new ArrayList<String>();
    private boolean loaded = false;
    private boolean instant = false;
    private boolean requireTicket = false;
    private int timeLeft;
    private int lastTime = -1;

    public void printInfo(final Player player)
    {
        player.sendMessage(ChatColor.GOLD + "Info for VoxelPort " + ChatColor.BLUE + "\"" + ChatColor.AQUA + this.getName() + ChatColor.BLUE + "\"" + ChatColor.GOLD + " :");
        player.sendMessage(ChatColor.GREEN + "This port" + ChatColor.AQUA + ":");
        player.sendMessage((this.instant() ? (ChatColor.LIGHT_PURPLE + "Is") : (ChatColor.RED + "Is not")) + ChatColor.GREEN + " instant");
        player.sendMessage((this.ticket() ? (ChatColor.LIGHT_PURPLE + "Requires") : (ChatColor.RED + "Does not require")) + ChatColor.GREEN + " a ticket");
        player.sendMessage((this.redstoneKey != null ? (ChatColor.LIGHT_PURPLE + "Has a Redstone Key set, which " + (this.redstoneKey.getBlock().isBlockPowered() ? (ChatColor.AQUA + "is powered") : (ChatColor.RED + "is not powered")))
                : (ChatColor.RED + "Does not have a Redstone Key")));
        player.sendMessage((!this.departures.isEmpty() ? (ChatColor.LIGHT_PURPLE + "Contains " + ChatColor.BLUE + this.departures.size() + ChatColor.LIGHT_PURPLE + " departure times.") : (ChatColor.RED + "Does not contain any departure times")));
        if (this.welcomeMessages.isEmpty())
        {
            player.sendMessage(ChatColor.RED + "Does not contain welcome messages");
        } else
        {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Contains " + ChatColor.BLUE + this.welcomeMessages.size() + ChatColor.LIGHT_PURPLE + " welcome messages, and they are" + ChatColor.GREEN + ":");
            this.welcomePlayer(player);
        }
    }

    class MoveEventSucks implements Runnable
    {

        private final Player player;
        private final Location loc;

        public MoveEventSucks(final Player player, final Location destination)
        {
            this.player = player;
            this.loc = destination;
        }

        @Override
        public void run()
        {
            this.player.teleport(this.loc, TeleportCause.ENDER_PEARL);
            this.player.sendMessage(ChatColor.DARK_AQUA + "Woosh!");

            this.player.getWorld().refreshChunk(this.player.getLocation().getBlockX(), this.player.getLocation().getBlockZ());
        }
    }

    //

    public Port(final String Name)
    {
        this.portalName = Name;
        this.readData();
    }

    public Port(final int highx, final int lowx, final int highy, final int lowy, final int highz, final int lowz, final String worldname, final String Name)
    {
        this(highx, lowx, highy, lowy, highz, lowz, worldname, Name, null, null);
    }

    public Port(final int highx, final int lowx, final int highy, final int lowy, final int highz, final int lowz, final String worldname, final String Name, final Location arrival, final Location depart)
    {
        this.portalZone = new Zone(highx, lowx, highy, lowy, highz, lowz, worldname);
        this.portalName = Name;
        this.arrivalLocation = arrival;
        this.departLocation = depart;
    }

    public Port(final int highx, final int lowx, final int highy, final int lowy, final int highz, final int lowz, final String worldname, final World.Environment env, final String Name)
    {
        this.portalZone = new Zone(highx, lowx, highy, lowy, highz, lowz, worldname, env);
        this.portalName = Name;
    }

    public String getName()
    {
        return this.portalName;
    }

    public void setZone(final Zone z)
    {
        this.portalZone = z;
    }

    public Zone getZone()
    {
        return this.portalZone;
    }

    public void setArrival(final Location loc)
    {
        this.arrivalLocation = loc;
    }

    public void setDestination(final Location loc)
    {
        this.departLocation = loc;
    }

    public Location getArrival()
    {
        return this.arrivalLocation;
    }

    public void setInstant(final boolean bool)
    {
        this.instant = bool;
    }

    public void setTicket(final boolean bool)
    {
        this.requireTicket = bool;
    }

    public void setRedstoneKey(final Location loc)
    {
        this.redstoneKey = loc;
    }

    public boolean instant()
    {
        return this.instant;
    }

    public boolean ticket()
    {
        return this.requireTicket;
    }

    public Location getRedstoneKey()
    {
        return this.redstoneKey;
    }

    public void clearMessages()
    {
        this.welcomeMessages.clear();
    }

    public void addMessage(final String message)
    {
        this.welcomeMessages.add(message);
    }

    public void welcomePlayer(final Player p)
    {
        if (this.welcomeMessages.isEmpty())
        {
            // p.sendMessage(ChatColor.DARK_GREEN +
            // "This port doesn't have a welcome message. Nag an admin to create one.");
            return;
        }
        for (final String s : this.welcomeMessages)
        {
            p.sendMessage(ChatColor.GREEN + s);
        }
    }

    public void clearDepartures()
    {
        this.departures.clear();
    }

    public void addDeparture(final int departure)
    {
        this.departures.add(departure);
    }

    public void setDepartures(final HashSet<Integer> hs)
    {
        for (final int i : hs)
        {
            this.departures.add(i);
        }
    }

    public void generateDepartures(final int start, final int interval, final Player player)
    {
        if (player == null)
        {
            int x;
            for (x = start; x < 24000; x += interval)
            {
                this.departures.add(x);
            }
            x -= 24000;
            if ((x >= 0) && (x < start))
            {
                for (x = x + 0; x < start; x += interval)
                {
                    this.departures.add(x);
                }
            }
        }
        if (this.departures.isEmpty())
        {
            if ((start > -1) && (start < 24000) && (interval > 0) && ((start % PortTick.codeTick) == 0) && ((interval % PortTick.codeTick) == 0))
            {
                if ((24000 % interval) == 0)
                {
                    int x;
                    for (x = start; x < 24000; x += interval)
                    {
                        this.departures.add(x);
                    }
                    x -= 24000;
                    if ((x >= 0) && (x < start))
                    {
                        for (int y = x; y < start; y += interval)
                        {
                            this.departures.add(y);
                        }
                    }
                    player.sendMessage(ChatColor.AQUA + "Created dispatch times for portal \"" + this.portalName + "\" starting at " + this.mcTimeFromCodeTime(start));
                    player.sendMessage(ChatColor.AQUA + "And departing every " + this.mcTimeFromCodeTime(interval));
                } else
                {
                    player.sendMessage(ChatColor.RED + "Irregular dispatch times are not supported. Please make sure the interval goes evenly into 24000.");
                }
            } else
            {
                player.sendMessage(ChatColor.RED + "Invalid start or interval value. Make sure they are bolth positive, a multiple of " + PortTick.codeTick + ", and under 24000");
            }
        } else
        {
            player.sendMessage(ChatColor.RED + "This Portal already contains departures! Please clear the departures with \"/vp disp [portalname] \"clear\"\" and try again.");
        }
    }

    private String mcTimeFromCodeTime(final int codeTime)
    {
        if (codeTime == 0)
        {
            return ChatColor.AQUA + "Never!";
        }
        int mctime = (int) (codeTime * 3.6D);
        final int seconds = mctime % 60;
        mctime = ((mctime - seconds) / 60);
        final int minutes = mctime % 60;
        final int hours = (mctime - minutes) / 60;
        if (hours == 0)
        {
            if (minutes == 0)
            {
                if (seconds == 0)
                {
                    return ChatColor.GREEN + "Now?";
                } else
                {
                    return ChatColor.GREEN + "" + seconds + ChatColor.BLUE + "s";
                }
            } else
            {
                if (seconds == 0)
                {
                    return ChatColor.GREEN + "" + minutes + ChatColor.BLUE + "m";
                } else
                {
                    return ChatColor.GREEN + "" + minutes + ChatColor.BLUE + "m" + ChatColor.DARK_BLUE + " : " + ChatColor.GREEN + "" + seconds + ChatColor.BLUE + "s";
                }
            }
        } else
        {
            if (minutes == 0)
            {
                if (seconds == 0)
                {
                    return ChatColor.GREEN + "" + hours + ChatColor.BLUE + "h";
                } else
                {
                    return ChatColor.GREEN + "" + hours + ChatColor.BLUE + "h" + ChatColor.DARK_BLUE + " : " + ChatColor.GREEN + "" + seconds + ChatColor.BLUE + "s";
                }
            } else
            {
                if (seconds == 0)
                {
                    return ChatColor.GREEN + "" + hours + ChatColor.BLUE + "h" + ChatColor.DARK_BLUE + " : " + ChatColor.GREEN + "" + minutes + ChatColor.BLUE + "m";
                } else
                {
                    return ChatColor.GREEN + "" + hours + ChatColor.BLUE + "h" + ChatColor.DARK_BLUE + " : " + ChatColor.GREEN + "" + minutes + ChatColor.BLUE + "m" + ChatColor.DARK_BLUE + " : " + ChatColor.GREEN + "" + seconds + ChatColor.BLUE
                            + "s";
                }
            }
        }
    }

    public String nextDeparture(final int currentTime)
    {
        int time = 0;
        for (int x = currentTime; x < 24000; x += PortTick.codeTick)
        {
            if (this.departures.contains(x))
            {
                return (this.mcTimeFromCodeTime(time));
            }
            time += PortTick.codeTick;
        }
        for (int x = 0; x < currentTime; x += PortTick.codeTick)
        {
            if (this.departures.contains(x))
            {
                return (this.mcTimeFromCodeTime(time));
            }
            time += PortTick.codeTick;
        }
        return ChatColor.AQUA + "Never";
    }

    public String nextDepartureTotal(final int currentTime)
    {
        // System.out.println("Time " + currentTime);
        int time = 0;
        final int roundedTime = currentTime - (currentTime % PortTick.codeTick);
        for (int x = roundedTime + PortTick.codeTick; x < 24000; x += PortTick.codeTick)
        {
            if (this.departures.contains(x))
            {
                final int i = time + (PortTick.codeTick - (currentTime % PortTick.codeTick));
                this.timeLeft = time;
                return this.mcTimeFromCodeTime(i);
            }
            time += PortTick.codeTick;
        }
        for (int x = 0; x < currentTime; x += PortTick.codeTick)
        {
            if (this.departures.contains(x))
            {
                final int i = time + (PortTick.codeTick - (currentTime % PortTick.codeTick));
                this.timeLeft = time;
                return this.mcTimeFromCodeTime(i);
            }
            time += PortTick.codeTick;
        }
        this.timeLeft = 0;
        return ChatColor.AQUA + "Never";
    }

    public boolean insideZone(final Location loc)
    {
        return this.portalZone.inZone(loc);
    }

    public boolean departPlayer(final Player p, final int time)
    {
        if (this.insideZone(p.getLocation()))
        {
            if (this.departures.contains(time))
            {
                if (this.departLocation == null)
                {
                    p.sendMessage(ChatColor.RED + "This portal doesn't contain a target location!");
                    PortTick.usedTickets.add(p);
                    return true;
                }
                p.teleport(this.departLocation, TeleportCause.ENDER_PEARL);
                PortTick.usedTickets.add(p);
                return true;
            } else
            {
                p.sendMessage(ChatColor.GOLD + "The next departure will occur in " + ChatColor.DARK_AQUA + ":     " + ChatColor.DARK_RED + "[" + this.mcTimeFromCodeTime(this.timeLeft) + ChatColor.DARK_RED + "]");
                if (time != this.lastTime)
                {
                    this.timeLeft -= PortTick.codeTick;
                    this.lastTime = time;
                }
                if (this.timeLeft < 0)
                {
                    this.timeLeft = 0;
                }
                return false;
            }
        } else
        {
            p.sendMessage(ChatColor.DARK_GREEN + "You have left the VoxelPort, therefore your ticket has expired.");
            PortTick.usedTickets.add(p);
            return true;
        }
    }

    public void instaPort(final Player p, final boolean override)
    {
        if (this.departLocation == null)
        {
            p.sendMessage(ChatColor.RED + "This portal doesn't contain a target location!");
            return;
        } else
        {
            if (override)
            {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(VoxelPort.voxelPort, new MoveEventSucks(p, this.departLocation));
            } else
            {
                if ((this.redstoneKey != null) && !this.redstoneKey.getBlock().isBlockPowered())
                {
                    return;
                }
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(VoxelPort.voxelPort, new MoveEventSucks(p, this.departLocation));
            }
        }
    }

    public boolean isPortActivated()
    {
        return this.redstoneKey == null ? true : this.redstoneKey.getBlock().isBlockPowered();
    }

    public void saveData()
    {
        try
        {
            final Path path = Paths.get(String.valueOf(VoxelPort.voxelPort.getDataFolder()), "ports", this.portalName + ".yml");
            final File f = path.toFile();
            if (!f.getParentFile().isDirectory())
            {
                f.mkdirs();
            }
            final YamlConfiguration portConfig = new YamlConfiguration();
            if (this.portalZone != null) {
                portConfig.set("zone.world", this.portalZone.world);
                portConfig.set("zone.high_x", this.portalZone.zonehighx);
                portConfig.set("zone.high_y", this.portalZone.zonehighy);
                portConfig.set("zone.high_z", this.portalZone.zonehighz);

                portConfig.set("zone.low_x", this.portalZone.zonelowx);
                portConfig.set("zone.low_y", this.portalZone.zonelowy);
                portConfig.set("zone.low_z", this.portalZone.zonelowz);
            }
            if (this.arrivalLocation != null)
            {
                portConfig.set("arrival.world", this.arrivalLocation.getWorld().getName());
                portConfig.set("arrival.x", this.arrivalLocation.getX());
                portConfig.set("arrival.y", this.arrivalLocation.getY());
                portConfig.set("arrival.z", this.arrivalLocation.getZ());
                portConfig.set("arrival.yaw", this.arrivalLocation.getYaw());
                portConfig.set("arrival.pitch", this.arrivalLocation.getPitch());
            }
            if (this.departLocation != null)
            {
                portConfig.set("depart.world", this.departLocation.getWorld().getName());
                portConfig.set("depart.x", this.departLocation.getX());
                portConfig.set("depart.y", this.departLocation.getY());
                portConfig.set("depart.z", this.departLocation.getZ());
                portConfig.set("depart.yaw", this.departLocation.getYaw());
                portConfig.set("depart.pitch", this.departLocation.getPitch());
            }
            if (this.redstoneKey != null)
            {
                portConfig.set("redstone.world", this.redstoneKey.getWorld().getName());
                portConfig.set("redstone.x", this.redstoneKey.getX());
                portConfig.set("redstone.y", this.redstoneKey.getY());
                portConfig.set("redstone.z", this.redstoneKey.getZ());
            }
            if (!this.departures.isEmpty())
            {
                portConfig.set("departures", this.departures);
            }
            if (!this.welcomeMessages.isEmpty())
            {
                portConfig.set("welcome", this.welcomeMessages);
            }

            portConfig.set("instant", this.instant);
            portConfig.set("require_ticket", this.requireTicket);
            portConfig.save(f);
            VoxelPort.log.info("Portal \"" + this.portalName + "\" saved.");
        }
        catch (final IOException e)
        {
            VoxelPort.log.warning("Error while saving portal \"" + this.portalName + "\"");
            e.printStackTrace();
        }
    }

    private void readData()
    {
        final Path path = Paths.get(String.valueOf(VoxelPort.voxelPort.getDataFolder()), "ports", this.portalName + ".yml");
        final File portFile = path.toFile();
        if (portFile.exists())
        {
                final YamlConfiguration portConfig = YamlConfiguration.loadConfiguration(portFile);

                if (portConfig.contains("zone")) {
                    final World world = Bukkit.getWorld(portConfig.getString("zone.world"));
                    final int highX = portConfig.getInt("zone.high_x");
                    final int highY = portConfig.getInt("zone.high_y");
                    final int highZ = portConfig.getInt("zone.high_z");

                    final int lowX = portConfig.getInt("zone.low_x");
                    final int lowY = portConfig.getInt("zone.low_y");
                    final int lowZ = portConfig.getInt("zone.low_z");

                    this.portalZone = new Zone(highX, lowX, highY, lowY, highZ, lowZ, world.getName(), world.getEnvironment());
                }

                if (portConfig.contains("arrival")) {
                    final World world = Bukkit.getWorld(portConfig.getString("arrival.world"));
                    final double wx = portConfig.getDouble("arrival.x");
                    final double wy = portConfig.getDouble("arrival.y");
                    final double wz = portConfig.getDouble("arrival.z");
                    final float wYaw = (float) portConfig.getDouble("arrival.yaw");
                    final float wPitch = (float) portConfig.getDouble("arrival.pitch");

                    this.arrivalLocation = new Location(world, wx, wy, wz, wYaw, wPitch);
                }

                if (portConfig.contains("depart")) {
                    final World world = Bukkit.getWorld(portConfig.getString("depart.world"));
                    final double wx = portConfig.getDouble("depart.x");
                    final double wy = portConfig.getDouble("depart.y");
                    final double wz = portConfig.getDouble("depart.z");
                    final float wYaw = (float) portConfig.getDouble("depart.yaw");
                    final float wPitch = (float) portConfig.getDouble("depart.pitch");

                    this.departLocation = new Location(world, wx, wy, wz, wYaw, wPitch);
                }

                if (portConfig.contains("redstone")) {
                    final World world = Bukkit.getWorld(portConfig.getString("redstone.world"));
                    final double wx = portConfig.getDouble("redstone.x");
                    final double wy = portConfig.getDouble("redstone.y");
                    final double wz = portConfig.getDouble("redstone.z");

                    this.redstoneKey = new Location(world, wx, wy, wz, 0, 0);
                }

                final List<Integer> departureList = portConfig.getIntegerList("departures");
                this.departures.addAll(new TreeSet<Integer>(departureList));

                final List<String> welcomeList = portConfig.getStringList("welcome");
                this.welcomeMessages.addAll(new TreeSet<String>(welcomeList));

                this.instant = portConfig.getBoolean("instant", false);
                this.requireTicket = portConfig.getBoolean("require_ticket", false);
                this.loaded = true;
            }
    }

    public boolean loaded()
    {
        return this.loaded;
    }

    public void genZoneBoundKeys()
    {
        final int contlowx = this.portalZone.zonelowx / PortManager.CONTAINER_SIZE;
        final int contlowz = this.portalZone.zonelowz / PortManager.CONTAINER_SIZE;
        final int conthighx = this.portalZone.zonehighx / PortManager.CONTAINER_SIZE;
        final int conthighz = this.portalZone.zonehighz / PortManager.CONTAINER_SIZE;
        for (int x = contlowx; x <= conthighx; x++)
        {
            for (int z = contlowz; z <= conthighz; z++)
            {
                PortManager.insertPort(this, x, z);
            }
        }
    }

    public void deleteZoneBoundKeys()
    {
        final int contlowx = this.portalZone.zonelowx / PortManager.CONTAINER_SIZE;
        final int contlowz = this.portalZone.zonelowz / PortManager.CONTAINER_SIZE;
        final int conthighx = this.portalZone.zonehighx / PortManager.CONTAINER_SIZE;
        final int conthighz = this.portalZone.zonehighz / PortManager.CONTAINER_SIZE;
        for (int x = contlowx; x <= conthighx; x++)
        {
            for (int z = contlowz; z <= conthighz; z++)
            {
                PortManager.deletePort(this, x, z);
            }
        }
    }
}
