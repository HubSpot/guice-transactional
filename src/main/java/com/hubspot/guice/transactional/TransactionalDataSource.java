package com.hubspot.guice.transactional;

import com.hubspot.guice.transactional.impl.TransactionalConnection;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class TransactionalDataSource implements DataSource {
  private static final ThreadLocal<TransactionalConnection> ACTIVE_TRANSACTION = new ThreadLocal<>();

  private final DataSource delegate;

  public TransactionalDataSource(DataSource delegate) {
    this.delegate = delegate;
  }

  public boolean inTransaction() {
    return getTransaction() != null;
  }

  public TransactionalConnection getTransaction() {
    return ACTIVE_TRANSACTION.get();
  }

  public TransactionalConnection pauseTransaction() {
    TransactionalConnection connection = getTransaction();
    ACTIVE_TRANSACTION.set(null);
    return connection;
  }

  public void resumeTransaction(TransactionalConnection connection) {
    ACTIVE_TRANSACTION.set(connection);
  }

  public void startTransaction() throws SQLException {
    if (inTransaction()) {
      throw new IllegalStateException("Already in a transaction");
    }
    ACTIVE_TRANSACTION.set(new TransactionalConnection(getConnection()));
  }

  public void commitTransaction() throws SQLException {
    if (!inTransaction()) {
      throw new IllegalStateException("No active transaction");
    }
    getTransaction().commit();
  }

  public void rollbackTransaction() throws SQLException {
    if (!inTransaction()) {
      throw new IllegalStateException("No active transaction");
    }
    getTransaction().rollback();
  }

  public void endTransaction() throws SQLException {
    if (!inTransaction()) {
      throw new IllegalStateException("No active transaction");
    }
    try {
      getTransaction().reallyClose();
    } finally {
      ACTIVE_TRANSACTION.set(null);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    TransactionalConnection connection = getTransaction();

    return connection == null ? delegate.getConnection() : connection;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    TransactionalConnection connection = getTransaction();

    return connection == null ? delegate.getConnection(username, password) : connection;
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
    return delegate.isWrapperFor(type);
  }

  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> type) throws SQLException {
    return delegate.unwrap(type);
  }
}
