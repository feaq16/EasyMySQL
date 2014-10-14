package net.feaq16.easymysql;

import org.bukkit.configuration.ConfigurationSection;

public interface MySQLWorker {

    public String getName();

    public void start();

    public void stop();

    public ConfigurationSection getConfig();

    public MySQLWorkerType getType();

    public void execute(MySQLExecutor query);

}
