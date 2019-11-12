package com.hubspot.guice.transactional.impl;

import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.sql.filter.FilterConnection;

public class TransactionalConnection extends FilterConnection {
  private final String databaseName;

  public TransactionalConnection(Connection delegate, String databaseName) throws SQLException {
    super(delegate);
    this.databaseName = databaseName;
    delegate.setAutoCommit(false);
  }

  public String getDatabaseName() {
    return databaseName;
  }

  @Override
  public void close() {
    // NO-OP, don't let QueryRunner or JDBI close the connection before the transaction is done
  }

  public void reallyClose() throws SQLException {
    getInner().setAutoCommit(true);
    super.close();
  }
}
