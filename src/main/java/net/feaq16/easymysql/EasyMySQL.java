package net.feaq16.easymysql;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import net.feaq16.easymysql.workers.DelayedPushWorker;
import net.feaq16.easymysql.workers.MultiThreadWorker;
import net.feaq16.easymysql.workers.SingleThreadWorker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class EasyMySQL extends JavaPlugin {

    private ArrayList<MySQLWorker> workers;

    @Override
    public void onEnable() {
        Timming t = new Timming(null).start();

        workers = new ArrayList<MySQLWorker>();

        File f = new File(this.getDataFolder() + File.separator + "config.yml");
        if (!f.exists()) {
            this.getConfig().options().copyHeader(true);
            this.getConfig().options().copyDefaults(true);
            this.getLogger().info("Generating " + f.getName() + "...");
            this.saveConfig();
        }

        ConfigurationSection cfgWorkers = this.getConfig().getConfigurationSection("workers");
        for (String s : cfgWorkers.getKeys(false)) {
            ConfigurationSection configuration = cfgWorkers.getConfigurationSection(s);

            String type = configuration.getString("type");
            if (type == null) {
                this.getLogger().warning("Worker type has a null value!");
                continue;
            }

            if (!configuration.getBoolean("enabled", true)) {
                this.getLogger().info("Skipping worker named " + s + " because it is not enabled!");
                continue;
            }

            this.getLogger().info("Creating worker named " + s + "...");
            MySQLWorker con;
            try {
                switch (type.toUpperCase()) {
                    case "MULTI_THREAD":
                        con = new MultiThreadWorker(this, configuration);
                        break;
                    case "SINGLE_THREAD":
                        con = new SingleThreadWorker(this, configuration);
                        break;
                    case "DELAYED_PUSH":
                        con = new DelayedPushWorker(this, configuration);
                        break;
                    default:
                        this.getLogger().warning("Can't find worker type named " + type);
                        continue;
                }
            } catch (Exception e) {
                this.getLogger().log(Level.WARNING, "Error while creating worker named " + s + "!", e);
                continue;
            }

            this.workers.add(con);

            this.getLogger().info("Starting worker named " + s + "...");
            con.start();

        }

        this.getLogger().info("Successfully loaded " + this.workers.size() + " workers!");
        this.getLogger().info("Loading time " + t.stop().getExecutingTime() + "ms");
    }

    @Override
    public void onDisable() {
        Timming t = new Timming(null).start();
        this.getLogger().info("Stopping " + this.workers.size() + " workers...");
        for (MySQLWorker con : this.workers) {
            con.stop();
        }
        this.getLogger().info("Disabling time " + t.stop().getExecutingTime() + "ms");
    }

    public MySQLWorker getWorker(String name) {
        if (name == null) {
            throw new NullPointerException("Worker name has a null value!");
        }
        for (MySQLWorker con : this.workers) {
            if (con.getName().equals(name)) {
                return con;
            }
        }

        return null;
    }

}
