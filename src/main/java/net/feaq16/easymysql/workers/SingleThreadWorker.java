package net.feaq16.easymysql.workers;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.feaq16.easymysql.MySQLWorker;
import net.feaq16.easymysql.MySQLWorkerType;
import net.feaq16.easymysql.MySQLExecutor;
import net.feaq16.easymysql.Timming;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class SingleThreadWorker implements MySQLWorker, Runnable {

    private final BoneCP pool;
    private volatile boolean running = false;
    private final BlockingQueue<MySQLExecutor> queries;
    private final ConfigurationSection cfg;
    private final Plugin pl;
    private final Thread thread;

    public SingleThreadWorker(Plugin pl, ConfigurationSection config) throws SQLException {
        this.cfg = config;
        this.pl = pl;
        this.thread = new Thread(this, "Single thread MySQL worker");
        this.queries = new ArrayBlockingQueue<MySQLExecutor>(config.getInt("queue-capacity", 10));

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
        return this.cfg.getName();
    }

    @Override
    public void run() {
        while (this.running || !this.queries.isEmpty()) {
            MySQLExecutor query;
            try {
                query = this.queries.take();
            } catch (Exception e) {
                pl.getLogger().log(Level.WARNING, "Error while getting query from queue!", e);
                continue;
            }

            pl.getLogger().info("Executing query using single thread method...");

            Timming timming = new Timming(null).start();
            try (Connection con = this.pool.getConnection()) {
                try (PreparedStatement stm = con.prepareStatement(query.getMySQLQuery())) {
                    query.execute(stm);
                }
            } catch (SQLException ex) {
                pl.getLogger().log(Level.WARNING, "Error while executing query!", ex);
            }

            pl.getLogger().info("Executing time: " + timming.stop().getExecutingTime() + "ms");
        }
    }

    @Override
    public void start() {
        if (this.running) {
            throw new IllegalStateException("Worker is already started!");
        }

        if (!this.queries.isEmpty()) {
            throw new IllegalStateException("Old executor don't done work!");
        }

        this.running = true;
        this.thread.start();
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
        return this.cfg;
    }

    @Override
    public MySQLWorkerType getType() {
        return MySQLWorkerType.SINGLE_THREAD;
    }

    @Override
    public void execute(final MySQLExecutor query) {
        if (query == null) {
            throw new NullPointerException("MySQLExecutor has a null value!");
        }

        if (!this.running) {
            throw new IllegalStateException("Worker isn't running!");
        }

        this.queries.add(query);
    }

}
