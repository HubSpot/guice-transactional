package com.hubspot.guice.transactional;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.slf4j.LoggerFactory;

import com.hubspot.guice.transactional.impl.TransactionalConnection;
import com.hubspot.guice.transactional.impl.TransactionalInterceptor;

public class TransactionalDataSource implements DataSource {
  private final AtomicReference<String> dbName = new AtomicReference<>();
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TransactionalDataSource.class);
  private final DataSource delegate;

  public TransactionalDataSource(DataSource delegate) {
    this.delegate = delegate;
  }

  @Override
  public Connection getConnection() throws SQLException {
    LOG.debug("Creating a new connection.", new RuntimeException("For the stack trace."));
    if (TransactionalInterceptor.inTransaction()) {
      TransactionalConnection connection = TransactionalInterceptor.getTransaction();
      LOG.debug("In transaction, found existing transcational connection {}.", connection);
      if (connection == null) {
        Connection delegateConnection = delegate.getConnection();
        ensureDbNamePopulated(delegateConnection);
        connection = new TransactionalConnection(delegateConnection, getDbName());
        TransactionalInterceptor.setTransaction(connection);
      }
      throwIfConnectionInvalid(connection);

      return connection;
    } else {
      LOG.debug("Getting regular connection.");
      Connection connection = delegate.getConnection();
      ensureDbNamePopulated(connection);
      return connection;
    }
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    if (TransactionalInterceptor.inTransaction()) {
      TransactionalConnection connection = TransactionalInterceptor.getTransaction();
      if (connection == null) {
        Connection delegateConnection = delegate.getConnection(username, password);
        ensureDbNamePopulated(delegateConnection);
        connection = new TransactionalConnection(delegateConnection, getDbName());
        TransactionalInterceptor.setTransaction(connection);
      }
      throwIfConnectionInvalid(connection);

      return connection;
    } else {
      Connection connection = delegate.getConnection(username, password);
      ensureDbNamePopulated(connection);
      return connection;
    }
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter printWriter) throws SQLException {
    delegate.setLogWriter(printWriter);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public boolean isWrapperFor(Class<?> type) throws SQLException {
    if (type.isInstance(this)) {
      return true;
    } else if (type.isInstance(delegate)) {
      return true;
    } else {
      return delegate.isWrapperFor(type);
    }
  }

  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> type) throws SQLException {
    if (type.isInstance(this)) {
      return (T) this;
    } else if (type.isInstance(delegate)) {
      return (T) delegate;
    } else {
      return delegate.unwrap(type);
    }
  }

  private void throwIfConnectionInvalid(TransactionalConnection connection) throws SQLException {
    String dbName = getDbName();
    if (!connection.getDatabaseName().equals(dbName)) {
      dbName = dbName != null ? dbName : "unknown";
      throw new SQLException(String.format("Attempt to acquire connection to database %s, during transaction in database %s",
          dbName, connection.getDatabaseName()));
    }
  }

  private String getDbName() {
    return dbName.get();
  }

  private void ensureDbNamePopulated(Connection connection) throws SQLException {
    dbName.set(connection.getCatalog());
  }
}
