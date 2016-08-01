package com.hubspot.guice.transactional;

import com.hubspot.guice.transactional.impl.TransactionalConnection;
import com.hubspot.guice.transactional.impl.TransactionalInterceptor;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class TransactionalDataSource implements DataSource {
  private final DataSource delegate;

  public TransactionalDataSource(DataSource delegate) {
    this.delegate = delegate;
  }

  @Override
  public Connection getConnection() throws SQLException {
    if (TransactionalInterceptor.inTransaction()) {
      TransactionalConnection connection = TransactionalInterceptor.getTransaction();
      if (connection == null) {
        connection = new TransactionalConnection(delegate.getConnection());
        TransactionalInterceptor.setTransaction(connection);
      }

      return connection;
    } else {
      return delegate.getConnection();
    }
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    if (TransactionalInterceptor.inTransaction()) {
      TransactionalConnection connection = TransactionalInterceptor.getTransaction();
      if (connection == null) {
        connection = new TransactionalConnection(delegate.getConnection(username, password));
        TransactionalInterceptor.setTransaction(connection);
      }

      return connection;
    } else {
      return delegate.getConnection();
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
