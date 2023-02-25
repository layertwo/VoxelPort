package com.thevoxelbox.voxelport;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * @author Voxel
 */
public class PortManager
{

    private static TreeMap<Integer, PortContainer> ports = new TreeMap<Integer, PortContainer>();
    private static HashMap<String, Port> reference = new HashMap<String, Port>();
    private static PortTick portTick;
    public static Material TICKET_BLOCK;
    public static Material ADMIN_BLOCK;
    public static Material PORT_IGNORE_BLOCK;
    public static Material BUTTON_BLOCK;
    public static byte BUTTON_BLOCK_DATA;
    public static int CONTAINER_SIZE;
    public static int CHECK_INTERVAL;
    public static int PORT_TICK_SPEED;
    private static HashMap<String, PortData> data = new HashMap<String, PortData>();
    private static VoxelPort plugin;

    public PortManager(final VoxelPort vp)
    {
        PortManager.plugin = vp;
        PortManager.loadConfig();
        PortManager.loadPortals();
        VoxelPort.log.info("Starting thread...");
        PortManager.portTick = new PortTick(PortManager.PORT_TICK_SPEED);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PortManager.plugin, PortManager.portTick, PortTick.codeTick, PortTick.codeTick <= 1 ? 2 : PortTick.codeTick);
        VoxelPort.log.info("Thread Started!");
    }

    public static void inBound(final Player player, final Location loc)
    {
        try
        {
            final PortContainer portCont = PortManager.ports.get((loc.getBlockX() / PortManager.CONTAINER_SIZE + ((loc.getBlockZ() / PortManager.CONTAINER_SIZE) * 10000)));
            if (portCont == null)
            {
                return;
            } else
            {
                portCont.check(player, loc);
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            return;
        }
    }

    public static Port getPort(final Location loc)
    {
        try
        {
            final PortContainer pc = PortManager.ports.get((loc.getBlockX() / PortManager.CONTAINER_SIZE + (loc.getBlockZ() / PortManager.CONTAINER_SIZE * 10000)));
            if (pc == null)
            {
                return null;
            } else
            {
                return pc.getPortAtLoc(loc);
            }
        }
        catch (final Exception e)
        {
            System.out.println("Exception getPort");
            e.printStackTrace();
            return null;
        }
    }

    private static void sortPorts()
    {
        for (final Port port : PortManager.reference.values())
        {
            port.genZoneBoundKeys();
        }
        if (PortManager.ports.isEmpty())
        {
            VoxelPort.log.warning("Portals have not been sorted.");
        } else
        {
            VoxelPort.log.info("Portal zones have been sorted into " + PortManager.ports.size() + " containers.");
        }
    }

    public static void insertPort(final Port n, final int x, final int z)
    {
        if (PortManager.ports.containsKey((x + (z * 10000))))
        {
            PortManager.ports.get((x + (z * 10000))).put(n);
        } else
        {
            PortManager.ports.put((x + (z * 10000)), new PortContainer(n));
        }
    }

    public static void deletePort(final Port oldPort, final int x, final int z)
    {
        try
        {
            if (PortManager.ports.get((x + (z * 10000))).remove(oldPort))
            {
                PortManager.ports.remove((x + (z * 10000)));
                VoxelPort.log.info("Removed x" + x + " z" + z + " " + oldPort.getName());
            } else
            {
                VoxelPort.log.info("Removed port from: x" + x + " z" + z + " " + oldPort.getName());
            }
        }
        catch (final Exception e)
        {
            VoxelPort.log.warning("Error: could not remove port");
            e.printStackTrace();
        }
    }

    private static void loadPortals()
    {
        try
        {
            final Path path = Paths.get(String.valueOf(VoxelPort.voxelPort.getDataFolder()), "ports");
            final File f = path.toFile();
            if (f.exists())
            {
                final File[] portFiles = f.listFiles();
                for (final File file : portFiles)
                {
                    final String baseName = file.getName().split("\\.")[0];
                    final Port newport = new Port(baseName);
                    if (newport.loaded())
                    {
                        PortManager.reference.put(newport.getName(), newport);
                    }
                }
                if (PortManager.reference.isEmpty())
                {
                    VoxelPort.log.info("No portals were found.");
                } else
                {
                    VoxelPort.log.info("Portals loaded! " + PortManager.reference.size() + " portals have been loaded.");
                    PortManager.sortPorts();
                }
            }
        }
        catch (final Exception e)
        {
            VoxelPort.log.warning("Error while loading VoxelPorts");
            e.printStackTrace();
        }
    }

    private static void loadConfig() {
        PortManager.plugin.saveDefaultConfig();
        final FileConfiguration config = PortManager.plugin.getConfig();
        // TODO handle exception where material does not exist
        PortManager.TICKET_BLOCK = Material.matchMaterial(config.getString("ticket-material"));
        PortManager.ADMIN_BLOCK = Material.matchMaterial(config.getString("admin-ticket-material"));
        PortManager.PORT_IGNORE_BLOCK = Material.matchMaterial(config.getString("port-ignore-material"));
        PortManager.BUTTON_BLOCK = Material.matchMaterial(config.getString("button-trigger-block"));
        PortManager.CONTAINER_SIZE = config.getInt("container-block-size");
        PortManager.CHECK_INTERVAL = config.getInt("walking-update-interval");
        PortManager.PORT_TICK_SPEED = config.getInt("port-tick-speed");
        if ((PortManager.PORT_TICK_SPEED % 50) != 0) {
            PortManager.plugin.onDisable();
            throw new IllegalArgumentException("port-tick-speed set to an invalid value!");
        }
        VoxelPort.log.info("TICKET_BLOCK set to " + PortManager.TICKET_BLOCK.name());
        VoxelPort.log.info("ADMIN_TICKET_BLOCK set to " + PortManager.ADMIN_BLOCK.name());
        VoxelPort.log.info("PORT_IGNORE_BLOCK set to " + PortManager.PORT_IGNORE_BLOCK.name());
        VoxelPort.log.info("BUTTON_BLOCK set to " + PortManager.BUTTON_BLOCK.name());
        VoxelPort.log.info("CONTAINER_SIZE set to " + PortManager.CONTAINER_SIZE);
        VoxelPort.log.info("CHECK_INTERVAL set to " + PortManager.CHECK_INTERVAL);
        VoxelPort.log.info("PORT_TICK_SPEED set to " + PortManager.PORT_TICK_SPEED);
    }

    public void manageCommand(final Player player, final String[] args)
    {
        /*
         *
         */
        if (args[0].equalsIgnoreCase("set"))
        {
            PortData portData = PortManager.data.get(player.getName());
            if (portData == null)
            {
                portData = new PortData(null);
            }

            if (args.length == 2)
            {
                final Port newPort = PortManager.reference.get(args[1]);
                portData.p = newPort;
                PortManager.data.put(player.getName(), portData);
                player.sendMessage(ChatColor.GREEN + "Current VoxelPort has been set to \"" + newPort.getName() + "\"");
                return;
            } else
            {
                final Port np = PortManager.getPort(player.getLocation());

                if (np == null)
                {
                    final Port npl = PortManager.getPort(new HitBlox(player, player.getWorld()).getTargetBlock().getLocation());

                    if (npl == null)
                    {
                        player.sendMessage(ChatColor.RED + "You are not standing or looking at a VoxelPort!");
                        return;
                    } else
                    {
                        portData.p = npl;
                        PortManager.data.put(player.getName(), portData);
                        player.sendMessage(ChatColor.GREEN + "Current VoxelPort has been set to VoxelPort you are looking at (" + npl.getName() + ")");
                        return;
                    }
                } else
                {
                    portData.p = np;
                    PortManager.data.put(player.getName(), portData);
                    player.sendMessage(ChatColor.GREEN + "Current VoxelPort has been set to VoxelPort at current location (" + np.getName() + ")");
                    return;
                }
            }
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("point"))
        {
            final Block target = new HitBlox(player, player.getWorld()).getTargetBlock();
            if (target == null)
            {
                player.sendMessage(ChatColor.RED + "This point is not valid!");
                return;
            }
            PortData pd = PortManager.data.get(player.getName());
            if (pd == null)
            {
                pd = new PortData(target);
                PortManager.data.put(player.getName(), pd);
                player.sendMessage(ChatColor.GREEN + "First point set.");
                return;
            }
            if (pd.a == null)
            {
                pd.a = target;
                player.sendMessage(ChatColor.GREEN + "First point set.");
            } else if (pd.b == null)
            {
                pd.b = target;
                player.sendMessage(ChatColor.GREEN + "Second point set.");
            } else
            {
                PortManager.data.put(player.getName(), new PortData(null));
                player.sendMessage(ChatColor.GREEN + "Port points cleared.");
            }
            return;
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("create"))
        {
            if (args.length < 2)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }

            final PortData pd = PortManager.data.get(player.getName());
            if ((pd == null) || (pd.a == null) || (pd.b == null))
            {
                player.sendMessage(ChatColor.RED + "Plese select two zone points before creating a portal.");
                return;
            }

            if (PortManager.reference.containsKey(args[1]))
            {
                player.sendMessage(ChatColor.DARK_RED + "A portal with this nane already exists!");
                return;
            }

            final Port np = new Port((pd.a.getX() > pd.b.getX() ? pd.a.getX() : pd.b.getX()), (pd.a.getX() < pd.b.getX() ? pd.a.getX() : pd.b.getX()), (pd.a.getY() > pd.b.getY() ? pd.a.getY() : pd.b.getY()), (pd.a.getY() < pd.b.getY() ? pd.a.getY()
                    : pd.b.getY()), (pd.a.getZ() > pd.b.getZ() ? pd.a.getZ() : pd.b.getZ()), (pd.a.getZ() < pd.b.getZ() ? pd.a.getZ() : pd.b.getZ()), pd.a.getWorld().getName(), pd.a.getWorld().getEnvironment(), args[1]);

            np.saveData();

            pd.p = np;
            pd.a = null;
            pd.b = null;
            np.genZoneBoundKeys();

            PortManager.reference.put(np.getName(), np);
            player.sendMessage(ChatColor.BLUE + "Portal \"" + np.getName() + "\" created successfully.");
            return;
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("target"))
        {
            if (args.length == 3)
            {
                final Port np = PortManager.reference.get(args[1]);
                if (np == null)
                {
                    player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                    return;
                }
                final Port tp = PortManager.reference.get(args[2]);
                if (tp == null)
                {
                    player.sendMessage(ChatColor.RED + "No port with name " + args[2] + " found.");
                    return;
                }
                if (tp.getArrival() == null)
                {
                    player.sendMessage(ChatColor.RED + "The target portal " + args[2] + " doesn't contain an arrival location");
                    return;
                }
                np.setDestination(tp.getArrival());
                np.saveData();
                player.sendMessage(ChatColor.GREEN + "target location for Port \"" + np.getName() + "\" has been set to arrival location of Port \"" + tp.getName() + "\"");
                return;
            }
            if (args.length < 2)
            {
                final PortData pd = PortManager.data.get(player.getName());
                if ((pd != null) && (pd.p != null))
                {
                    pd.p.setDestination(player.getLocation());
                    pd.p.saveData();
                    player.sendMessage(ChatColor.GREEN + "Target location for Port \"" + pd.p.getName() + "\" has been set to current location.");
                } else
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                }
                return;
            }

            final Port np = PortManager.reference.get(args[1]);

            if (np != null)
            {
                np.setDestination(player.getLocation());
                np.saveData();
                player.sendMessage(ChatColor.GREEN + "Target location for Port \"" + np.getName() + "\" has been set to current location.");
                return;
            } else
            {
                player.sendMessage(ChatColor.RED + "A port with this name doesn't exist.");
                return;
            }
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("targetWorld"))
        {
            if (args.length < 3)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            } else
            {
                final Port np = PortManager.reference.get(args[1]);
                if (np == null)
                {
                    player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                    return;
                }
                if (args.length == 4)
                {
                    switch (args[3].charAt(0))
                    {
                        case 'd':
                            np.setDestination(Bukkit.getWorld(args[2]).getSpawnLocation());
                            np.saveData();
                            player.sendMessage(ChatColor.GREEN + "Target location for Port \"" + np.getName() + "\" has been set to origin of world \"" + args[2] + "\"");
                            return;

                        case 'n':
                            np.setDestination(Bukkit.getWorld(args[2]).getSpawnLocation());
                            np.saveData();
                            player.sendMessage(ChatColor.GREEN + "Target location for Port \"" + np.getName() + "\" has been set to origin of nether world \"" + args[2] + "\"");
                            return;

                        default:
                            player.sendMessage(ChatColor.RED + "Invalid parameters! use \"default\" or \"nether\"");
                            return;
                    }
                } else
                {
                    np.setDestination(Bukkit.getWorld(args[2]).getSpawnLocation());
                    np.saveData();
                    player.sendMessage(ChatColor.GREEN + "Target location for Port \"" + np.getName() + "\" has been set to origin of world \"" + args[2] + "\"");
                    return;
                }
            }
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("arrive"))
        {
            if (args.length < 2)
            {
                final PortData pd = PortManager.data.get(player.getName());
                if ((pd != null) && (pd.p != null))
                {
                    pd.p.setArrival(player.getLocation());
                    pd.p.saveData();
                    player.sendMessage(ChatColor.GREEN + "Arrival location for Port \"" + pd.p.getName() + "\" has been set to current location.");
                } else
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                }
                return;
            }

            final Port np = PortManager.reference.get(args[1]);

            if (np != null)
            {
                np.setArrival(player.getLocation());
                np.saveData();
                player.sendMessage(ChatColor.GREEN + "Arrival location for Port \"" + np.getName() + "\" has been set to current location.");
            } else
            {
                player.sendMessage(ChatColor.RED + "A port with this name doesn't exist.");
            }
            return;
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("info"))
        {
            if (args.length < 2)
            {
                final PortData pd = PortManager.data.get(player.getName());
                if ((pd != null) && (pd.p != null))
                {
                    pd.p.printInfo(player);
                } else
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                }
                return;
            }

            final Port np = PortManager.reference.get(args[1]);

            if (np != null)
            {
                np.printInfo(player);
            } else
            {
                player.sendMessage(ChatColor.RED + "A port with this name doesn't exist.");
            }
            return;
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("disp"))
        {
            if (args.length < 3)
            {
                final PortData pd = PortManager.data.get(player.getName());
                if ((pd != null) && (pd.p != null))
                {
                    if (args[1].equalsIgnoreCase("clear"))
                    {
                        pd.p.clearDepartures();
                        player.sendMessage(ChatColor.GREEN + "Departures for port \"" + pd.p.getName() + "\" cleared.");
                        return;
                    }
                    pd.p.addDeparture(Integer.parseInt(args[1]));
                    pd.p.saveData();
                    player.sendMessage(ChatColor.GREEN + "Departures time " + args[1] + " added to port \"" + pd.p.getName() + "\"");
                } else
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                }
                return;
            }

            final Port np = PortManager.reference.get(args[1]);

            if (np != null)
            {
                if (args[2].equalsIgnoreCase("clear"))
                {
                    np.clearDepartures();
                    player.sendMessage(ChatColor.GREEN + "Departures for port \"" + np.getName() + "\" cleared.");
                    return;
                }
                np.addDeparture(Integer.parseInt(args[2]));
                np.saveData();
                player.sendMessage(ChatColor.GREEN + "Departure time " + args[2] + " added to port \"" + np.getName() + "\"");
            } else
            {
                player.sendMessage(ChatColor.RED + "A port with this name doesn't exist.");
            }
            return;
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("genDisp"))
        {
            if (args.length < 3)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }

            final Port np = PortManager.reference.get(args[1]);
            if (np == null)
            {
                player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                return;
            }

            if (args.length == 3)
            {
                np.generateDepartures(0, Integer.parseInt(args[2]), player);
                np.saveData();
                player.sendMessage(ChatColor.GREEN + "Dispatch times for VoxelPort \"" + np.getName() + "\" have been generated starting at 0 with intervals of " + args[2]);
            } else if (args.length == 4)
            {
                np.generateDepartures(Integer.parseInt(args[3]), Integer.parseInt(args[2]), player);
                np.saveData();
                player.sendMessage(ChatColor.GREEN + "Dispatch times for VoxelPort \"" + np.getName() + "\" have been generated starting at " + args[3] + " with intervals of " + args[2]);
            }
            return;
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("instaPort"))
        {
            if (args.length < 2)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }
            if (args.length == 2)
            {
                final PortData pd = PortManager.data.get(player.getName());
                if ((pd != null) && (pd.p != null))
                {
                    pd.p.setInstant(Boolean.parseBoolean(args[1]));
                    pd.p.saveData();
                    player.sendMessage(ChatColor.GREEN + "InstaPort has been set to " + args[1] + " for VoxelPort \"" + pd.p.getName() + "\"");
                } else
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                }
                return;
            }

            final Port np = PortManager.reference.get(args[1]);
            if (np == null)
            {
                player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                return;
            }

            np.setInstant(Boolean.parseBoolean(args[2]));
            np.saveData();
            player.sendMessage(ChatColor.GREEN + "InstaPort has been set to " + args[2] + " for VoxelPort \"" + np.getName() + "\"");
            return;
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("requireTicket"))
        {
            if (args.length < 2)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }
            if (args.length == 2)
            {
                final PortData pd = PortManager.data.get(player.getName());
                if ((pd != null) && (pd.p != null))
                {
                    pd.p.setTicket(Boolean.parseBoolean(args[1]));
                    pd.p.saveData();
                    player.sendMessage(ChatColor.GREEN + "TicketRequirement has been set to " + args[1] + " for VoxelPort \"" + pd.p.getName() + "\"");
                } else
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                }
                return;
            }

            final Port np = PortManager.reference.get(args[1]);
            if (np == null)
            {
                player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                return;
            }

            np.setTicket(Boolean.parseBoolean(args[2]));
            np.saveData();
            player.sendMessage(ChatColor.GREEN + "TicketRequirement has been set to " + args[2] + " for VoxelPort \"" + np.getName() + "\"");
            return;
        }
        /*
         *
         */
        // if (s[0].equalsIgnoreCase("depart")) {
        // newPort np = getPort(p.getLocation());
        // if (np == null) {
        // p.sendMessage(ChatColor.RED + "You are not inside a VoxelPort!");
        // return;
        // } else {
        // np.instaPort(p);
        // return;
        // }
        // }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("welcomeClear"))
        {
            if (args.length < 2)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }

            final Port np = PortManager.reference.get(args[1]);
            if (np == null)
            {
                player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                return;
            } else
            {
                np.clearMessages();
                np.saveData();
                player.sendMessage(ChatColor.GREEN + "Welcome messages for VoxelPort " + np.getName() + " have been cleared!");
                return;
            }
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("welcome"))
        {
            if (args.length <= 2)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }

            final Port np = PortManager.reference.get(args[1]);
            if (np == null)
            {
                player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                return;
            } else
            {
                String message = "";
                for (int x = 2; x < args.length; x++)
                {
                    if (args[x].startsWith("#$"))
                    {
                        if (Character.isDigit(args[x].charAt(2)))
                        {
                            message += ChatColor.getByChar(args[x].charAt(2));
                            message += args[x].substring(3) + " ";
                        } else
                        {
                            final char c = args[x].charAt(2);

                            switch (c)
                            {
                                case 'a':
                                    message += ChatColor.getByChar(c);
                                    message += args[x].substring(3) + " ";
                                    break;

                                case 'b':
                                    message += ChatColor.getByChar(c);
                                    message += args[x].substring(3) + " ";
                                    break;

                                case 'c':
                                    message += ChatColor.getByChar(c);
                                    message += args[x].substring(3) + " ";
                                    break;

                                case 'd':
                                    message += ChatColor.getByChar(c);
                                    message += args[x].substring(3) + " ";
                                    break;

                                case 'e':
                                    message += ChatColor.getByChar(c);
                                    message += args[x].substring(3) + " ";
                                    break;

                                case 'f':
                                    message += ChatColor.getByChar(c);
                                    message += args[x].substring(3) + " ";
                                    break;

                                default:
                                    player.sendMessage(ChatColor.RED + "Invalid Colour String! \"" + args[x] + "\"");
                                    return;
                            }
                        }
                    } else
                    {
                        message += args[x] + " ";
                    }
                }
                np.addMessage(message);
                np.saveData();
                player.sendMessage(ChatColor.BLUE + "Message added to VoxelPort " + np.getName());
                player.sendMessage(message);
                return;
            }
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("delete"))
        {
            if (args.length < 2)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }

            final Port newPort = PortManager.reference.get(args[1]);
            if (newPort == null)
            {
                player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                return;
            } else
            {
                final Path path = Paths.get(String.valueOf(VoxelPort.voxelPort.getDataFolder()), "ports", newPort.getName());
                final File port = path.toFile();
                if (PortTick.tickets.containsValue(newPort))
                {
                    PortTick.removeTicketsFor(newPort.getName());
                }
                newPort.deleteZoneBoundKeys();
                PortManager.reference.remove(newPort.getName());
                port.delete();
                player.sendMessage(ChatColor.GREEN + "VoxelPort " + newPort.getName() + " has been deleted!");
                return;
            }
        }
        /*
         *
         */
        if (args[0].equalsIgnoreCase("zone"))
        {
            final PortData portData = PortManager.data.get(player.getName());
            if ((portData == null) || (portData.a == null) || (portData.b == null))
            {
                player.sendMessage(ChatColor.RED + "Please select two zone points before changing the zone.");
                return;
            }

            if (args.length < 2)
            {
                if (portData.p == null)
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                    return;
                }

                portData.p.setZone(new Zone((portData.a.getX() > portData.b.getX() ? portData.a.getX() : portData.b.getX()), (portData.a.getX() < portData.b.getX() ? portData.a.getX() : portData.b.getX()), (portData.a.getY() > portData.b.getY() ? portData.a.getY() : portData.b.getY()), (portData.a.getY() < portData.b.getY() ? portData.a.getY()
                        : portData.b.getY()), (portData.a.getZ() > portData.b.getZ() ? portData.a.getZ() : portData.b.getZ()), (portData.a.getZ() < portData.b.getZ() ? portData.a.getZ() : portData.b.getZ()), portData.a.getWorld().getName(), portData.a.getWorld().getEnvironment()));
                portData.p.saveData();
                player.sendMessage(ChatColor.GREEN + "Zone set for VoxelPort " + ChatColor.GOLD + portData.p.getName());
                return;
            } else
            {
                final Port np = PortManager.reference.get(args[1]);
                if (np == null)
                {
                    player.sendMessage(ChatColor.RED + "No port with name " + args[1] + " found.");
                    return;
                } else
                {
                    np.setZone(new Zone((portData.a.getX() > portData.b.getX() ? portData.a.getX() : portData.b.getX()), (portData.a.getX() < portData.b.getX() ? portData.a.getX() : portData.b.getX()), (portData.a.getY() > portData.b.getY() ? portData.a.getY() : portData.b.getY()), (portData.a.getY() < portData.b.getY() ? portData.a
                            .getY() : portData.b.getY()), (portData.a.getZ() > portData.b.getZ() ? portData.a.getZ() : portData.b.getZ()), (portData.a.getZ() < portData.b.getZ() ? portData.a.getZ() : portData.b.getZ()), portData.a.getWorld().getName(), portData.a.getWorld().getEnvironment()));
                    np.saveData();
                    player.sendMessage(ChatColor.GREEN + "Zone set for VoxelPort " + ChatColor.GOLD + np.getName());
                    return;
                }
            }
        }
        if (args[0].equalsIgnoreCase("redstoneKey"))
        {
            if (args.length == 1)
            {
                player.sendMessage(ChatColor.RED + "Invalid number of arguments!");
                return;
            }

            final PortData pd = PortManager.data.get(player.getName());

            if (args[1].equalsIgnoreCase("set"))
            {
                if ((pd == null) || (pd.a == null))
                {
                    player.sendMessage(ChatColor.RED + "Please select Block A with /point first!");
                    return;
                }

                if (args.length == 3)
                {
                    final Port np = PortManager.reference.get(args[2]);
                    if (np == null)
                    {
                        player.sendMessage(ChatColor.RED + "No port with name " + args[2] + " found.");
                        return;
                    }

                    np.setRedstoneKey(pd.a.getLocation());
                    pd.a = null;
                    np.saveData();
                    player.sendMessage(ChatColor.GREEN + "RedstoneKey set for VoxelPort " + ChatColor.GOLD + np.getName());
                    return;
                }

                if (pd.p == null)
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                    return;
                }

                pd.p.setRedstoneKey(pd.a.getLocation());
                pd.a = null;
                pd.p.saveData();
                player.sendMessage(ChatColor.GREEN + "RedstoneKey set for VoxelPort " + ChatColor.GOLD + pd.p.getName());
                return;
            } else if (args[1].equalsIgnoreCase("clear"))
            {
                if (args.length == 3)
                {
                    final Port np = PortManager.reference.get(args[2]);
                    if (np == null)
                    {
                        player.sendMessage(ChatColor.RED + "No port with name " + args[2] + " found.");
                        return;
                    }

                    np.setRedstoneKey(null);
                    np.saveData();
                    player.sendMessage(ChatColor.GREEN + "RedstoneKey cleared for VoxelPort " + ChatColor.GOLD + np.getName());
                    return;
                }

                if ((pd == null) || (pd.p == null))
                {
                    player.sendMessage(ChatColor.RED + "You haven't set a port, please pick a portal name.");
                    return;
                }

                pd.p.setRedstoneKey(null);
                pd.p.saveData();
                player.sendMessage(ChatColor.GREEN + "RedstoneKey cleared for VoxelPort " + ChatColor.GOLD + pd.p.getName());
                return;
            }

            return;
        }
    }

    private class PortData
    {

        public PortData(final Block t)
        {
            this.a = t;
        }

        public Block a;
        public Block b;
        public Port p;
    }
}
