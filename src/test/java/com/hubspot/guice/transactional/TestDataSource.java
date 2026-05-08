package com.hubspot.guice.transactional;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class TestDataSource implements DataSource {

    private final String name;

    public TestDataSource(String name) {
      this.name = name;
    }

    @Override
    public Connection getConnection() throws SQLException {
      return new TestConnection(name);
    }

    @Override
    public Connection getConnection(String username, String password)
        throws SQLException {
      return new TestConnection(name);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {}

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {}

    @Override
    public int getLoginTimeout() throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException();
    }

}
