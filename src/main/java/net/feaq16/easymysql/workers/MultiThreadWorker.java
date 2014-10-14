package net.feaq16.easymysql.workers;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.feaq16.easymysql.MySQLWorker;
import net.feaq16.easymysql.MySQLWorkerType;
import net.feaq16.easymysql.MySQLExecutor;
import net.feaq16.easymysql.Timming;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class MultiThreadWorker implements MySQLWorker {

    private final BoneCP pool;
    private final ConfigurationSection config;
    private boolean running = false;
    private final Plugin pl;

    public MultiThreadWorker(Plugin pl, ConfigurationSection config) throws SQLException {
        this.config = config;
        this.pl = pl;

        BoneCPConfig cfg = new BoneCPConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + config.getString("MySQL.host") + "/" + config.getString("MySQL.database"));
        cfg.setUsername(config.getString("MySQL.user"));
        cfg.setPassword(config.getString("MySQL.password"));
        cfg.setMaxConnectionsPerPartition(config.getInt("connections.max-per-partition", 3));
        cfg.setMinConnectionsPerPartition(config.getInt("connections.min-per-partition", 1));
        cfg.setPartitionCount(config.getInt("connections.partition-count", 2));

        this.pool = new BoneCP(cfg);
    }

    @Override
    public String getName() {
        return this.config.getName();
    }

    @Override
    public void start() {
        if (this.running) {
            throw new IllegalStateException("Worker is already running!");
        }

        this.running = true;
    }

    @Override
    public void stop() {
        if (!this.running) {
            throw new IllegalStateException("Worker isn't running!");
        }

        this.running = false;
    }

    @Override
    public ConfigurationSection getConfig() {
        return this.config;
    }

    @Override
    public MySQLWorkerType getType() {
        return MySQLWorkerType.MULTI_THREAD;
    }

    @Override
    public void execute(final MySQLExecutor query) {
        if (query == null) {
            throw new NullPointerException("MySQLExecutor has a null value!");
        }

        new Thread("Multi thread MySQL worker") {
            @Override
            public void run() {
                pl.getLogger().info("Executing query using multi thread method...");
                Timming timming = new Timming(null).start();
                try (Connection con = pool.getConnection()) {
                    try (PreparedStatement stm = con.prepareStatement(query.getMySQLQuery())) {
                        query.execute(stm);
                    }
                } catch (SQLException ex) {
                    pl.getLogger().log(Level.WARNING, "Error while executing query!", ex);
                }

                pl.getLogger().info("Executing time: " + timming.stop().getExecutingTime() + "ms");
            }
        }.start();
    }

}
