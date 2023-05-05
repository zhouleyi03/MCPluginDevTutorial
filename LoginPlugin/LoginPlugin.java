// by zhouleyi on 2023-5-5 @ZJU
package zhouleyi.loginplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;

public final class LoginPlugin extends JavaPlugin implements Listener {

    private HashMap<UUID, UserData> user_data;

    public static String getMD5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(s.getBytes());
            byte[] md5 = digest.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : md5) {
                if (b < 0)
                    b += 256;
                result.append(Integer.toHexString(b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Error occurred during md5 encryption!");
        }
    }

    public void loadUserData() {
        String path = getDataFolder() + File.separator + "user_data.dat";
        if (new File(path).exists()) { // 首次启动时文件并不存在，因此需要预先判断。
            try {
                FileInputStream fis = new FileInputStream(path);
                ObjectInputStream ois = new ObjectInputStream(fis);
                user_data = (HashMap<UUID, UserData>) ois.readObject();
                ois.close();
                fis.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveUserData() {
        String path = getDataFolder() + File.separator + "user_data.dat";
        try {
            getDataFolder().mkdirs(); // 如果没有事先创建目录，实测下面的操作可能会失败。
            FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(user_data);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        user_data = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        loadUserData();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        saveUserData();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!user_data.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "[LoginPlugin] Please register to get full access to the functions of this server!");
            player.sendMessage("Use " + ChatColor.AQUA + "\"/register <password>\"" + ChatColor.WHITE + " to" +
                    " register.");
        } else {
            UserData userdata = user_data.get(uuid);
            if (System.currentTimeMillis() - userdata.last_login_timestamp <= 86400) {
                player.sendMessage(ChatColor.AQUA + "[LoginPlugin] Welcome to this server!");
            } else {
                player.sendMessage(ChatColor.RED + "[LoginPlugin] Please login first.");
                player.sendMessage("Use " + ChatColor.AQUA + "\"/login <password>" + ChatColor.WHITE + " to login.\"");
            }
        }
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!user_data.containsKey(uuid)) {
            event.setCancelled(true);
        } else {
            UserData userdata = user_data.get(uuid);
            if (System.currentTimeMillis() - userdata.last_login_timestamp > 86400) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("register")) {
            if (!(sender instanceof Player))
                sender.sendMessage(ChatColor.RED + "[LoginPlugin] This command can only be issued by a player!");
            else {
                if (user_data.containsKey(((Player) sender).getUniqueId()))
                    sender.sendMessage("[LoginPlugin] You have registered already!");
                else {
                    if (args.length > 1)
                        return false;
                    else {
                        UserData userdata = new UserData(getMD5(args[0]), 0);
                        user_data.put(((Player) sender).getUniqueId(), userdata);
                        sender.sendMessage("[LoginPlugin] Successfully registered. Now you can use " + ChatColor.AQUA + "\"/login <password>\"" + ChatColor.WHITE + " to login.");
                    }
                }
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("login")) {
            if (!(sender instanceof Player))
                sender.sendMessage(ChatColor.RED + "[LoginPlugin] This command can only be issued by a player!");
            else {
                if (args.length > 1)
                    return false;
                else {
                    if (!user_data.containsKey(((Player) sender).getUniqueId())) {
                        sender.sendMessage("[LoginPlugin] You have not registered. Please register first!");
                        sender.sendMessage("Use " + ChatColor.AQUA + "\"/register <password>\"" + ChatColor.WHITE + " to" +
                                " register.");
                    } else if (System.currentTimeMillis() - user_data.get(((Player) sender).getUniqueId()).last_login_timestamp <= 86400) {
                        sender.sendMessage("[LoginPlugin] You have logged-in already!");
                    } else if (user_data.get(((Player) sender).getUniqueId()).encrypted_password.equals(getMD5(args[0]))) {
                        UserData userdata = new UserData(getMD5(args[0]), System.currentTimeMillis());
                        user_data.put(((Player) sender).getUniqueId(), userdata);
                        sender.sendMessage("[LoginPlugin] Successfully logged-in. HAVE FUN!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "[LoginPlugin] Incorrect password.");
                    }
                }
            }
            return true;
        }
        return false;
    }
}