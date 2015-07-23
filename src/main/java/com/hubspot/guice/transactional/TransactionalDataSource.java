package com.hubspot.guice.transactional;

import com.hubspot.guice.transactional.impl.TransactionalConnection;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class TransactionalDataSource implements DataSource, Closeable {
  private static final ThreadLocal<TransactionalConnection> ACTIVE_TRANSACTION = new ThreadLocal<TransactionalConnection>();

  private final DataSource delegate;

  public TransactionalDataSource(DataSource delegate) {
    this.delegate = delegate;
  }

  public boolean inTransaction() {
    return ACTIVE_TRANSACTION.get() != null;
  }

  public TransactionalConnection pauseTransaction() {
    TransactionalConnection connection = ACTIVE_TRANSACTION.get();
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
    ACTIVE_TRANSACTION.get().commit();
  }

  public void rollbackTransaction() throws SQLException {
    if (!inTransaction()) {
      throw new IllegalStateException("No active transaction");
    }
    ACTIVE_TRANSACTION.get().rollback();
  }

  public void endTransaction() throws SQLException {
    if (!inTransaction()) {
      throw new IllegalStateException("No active transaction");
    }
    try {
      ACTIVE_TRANSACTION.get().reallyClose();
    } finally {
      ACTIVE_TRANSACTION.set(null);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    TransactionalConnection connection = ACTIVE_TRANSACTION.get();

    return connection == null ? delegate.getConnection() : connection;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    TransactionalConnection connection = ACTIVE_TRANSACTION.get();

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

  @Override
  public void close() throws IOException {
    if (delegate instanceof Closeable) {
      ((Closeable) delegate).close();
    }
  }
}
