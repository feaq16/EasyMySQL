package net.feaq16.easymysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface MySQLExecutor {

    public String getMySQLQuery();

    public void execute(PreparedStatement stm) throws SQLException;

}
