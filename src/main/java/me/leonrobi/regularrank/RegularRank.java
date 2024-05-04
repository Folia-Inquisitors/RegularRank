package me.leonrobi.regularrank;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class RegularRank extends JavaPlugin implements Listener {

    private FileConfiguration config = this.getConfig();
    private FileConfiguration dataConfig;
    private File dataFile;

    private static RegisteredServiceProvider<LuckPerms> PROVIDER;
    private static LuckPerms LUCK_PERMS;
    private static InheritanceNode GROUP_NODE;

    private void severe(String msg) {
        getLogger().severe(msg + " Please contact leonrobi to fix!");
    }

    @Override
    public void onEnable() {
        config.addDefault("joins-for-regular", 3);
        config.addDefault("regular-role-name-luckperms", "regular");
        config.options().copyDefaults(true);
        this.saveConfig();

        File file = new File(getDataFolder() + "/data.yml");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    severe("Failed to create file.");
                    throw new RuntimeException();
                }
            } catch (IOException e) {
                severe("Failed to create file (e).");
                throw new RuntimeException(e);
            }
        }

        dataFile = file;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        PROVIDER = Bukkit.getServicesManager().getRegistration(LuckPerms.class);;

        if (PROVIDER == null) {
            severe("PROVIDER not found!");
            throw new RuntimeException();
        }

        try {
            LUCK_PERMS = PROVIDER.getProvider();
        } catch (NullPointerException e) {
            severe("LuckPerms not found!");
            throw new RuntimeException();
        }

        GROUP_NODE = InheritanceNode.builder(getRegularRoleName()).value(true).build();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            severe("Failed to save data.yml.");
            throw new RuntimeException(e);
        }
    }

    public String getRegularRoleName() {
        return config.getString("regular-role-name-luckperms");
    }

    public int getJoinForRegular() {
        return config.getInt("joins-for-regular");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        String key = player.getUniqueId().toString();
        int newCount;
        if (!dataConfig.contains(key)) {
            newCount = 1;
            dataConfig.set(key, newCount);
        } else {
            newCount = dataConfig.getInt(key) + 1;
            dataConfig.set(key, newCount);
        }

        if (newCount >= getJoinForRegular()) {
            User user = LUCK_PERMS.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                severe("Could not find LuckPerms user for player: " + player.getName());
                return;
            }

            DataMutateResult result = user.data().add(GROUP_NODE);

            if (!result.wasSuccessful()) {
                severe("Not successful");
            }

            LUCK_PERMS.getUserManager().saveUser(user);
        }
    }
}
