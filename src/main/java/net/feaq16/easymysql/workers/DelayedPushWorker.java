package net.feaq16.easymysql.workers;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;
import net.feaq16.easymysql.MySQLWorker;
import net.feaq16.easymysql.MySQLWorkerType;
import net.feaq16.easymysql.MySQLExecutor;
import net.feaq16.easymysql.Timming;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class DelayedPushWorker implements MySQLWorker, Runnable {

    private final BoneCP pool;
    private volatile boolean running = true;
    private final ConfigurationSection config;
    private final LinkedList<MySQLExecutor> queries;
    private final Thread thread;
    private final Plugin pl;
    private final long delay;

    public DelayedPushWorker(Plugin pl, ConfigurationSection section) throws SQLException {
        this.config = section;
        this.pl = pl;
        this.thread = new Thread(this, "Delayed push MySQL worker");
        this.queries = new LinkedList<MySQLExecutor>();
        this.delay = getConfig().getLong("push-delay", 1000);

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
        return config.getName();
    }

    @Override
    public void run() {
        while (this.running || !this.queries.isEmpty()) {
            final LinkedList<MySQLExecutor> queries;
            synchronized (this.queries) {
                queries = new LinkedList<MySQLExecutor>(this.queries);
                this.queries.clear();
            }

            pl.getLogger().info("Executing " + queries.size() + " queries using delayed push method...");

            Timming timming = new Timming(null).start();

            try (Connection con = this.pool.getConnection()) {
                con.setAutoCommit(false);

                for (MySQLExecutor query : queries) {
                    try (PreparedStatement stm = con.prepareStatement(query.getMySQLQuery())) {
                        query.execute(stm);
                    } catch (Exception e) {
                        pl.getLogger().log(Level.WARNING, "Error while performing query!", e);
                    }
                }

                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                pl.getLogger().log(Level.WARNING, "Error while performing queries!", ex);
            }

            pl.getLogger().info("Executing time: " + timming.stop().getExecutingTime() + "ms");

            long sleep = this.delay - delay;
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (Exception e) {
                    pl.getLogger().log(Level.WARNING, "Error while sleeping thread!", e);
                }
            }

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
        return config;
    }

    @Override
    public MySQLWorkerType getType() {
        return MySQLWorkerType.DELAYED_PUSH;
    }

    @Override
    public void execute(MySQLExecutor query) {
        if (query == null) {
            throw new NullPointerException("MySQLExecutor has a null value!");
        }

        if (!this.running) {
            throw new IllegalStateException("Worker isn't running!");
        }

        synchronized (this.queries) {
            this.queries.add(query);
        }
    }

}
