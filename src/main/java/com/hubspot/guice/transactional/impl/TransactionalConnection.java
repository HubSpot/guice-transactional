package com.hubspot.guice.transactional.impl;

import com.mchange.v2.sql.filter.FilterConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionalConnection extends FilterConnection {

  public TransactionalConnection(Connection delegate) throws SQLException {
    super(delegate);
    delegate.setAutoCommit(false);
  }

  @Override
  public void close() {
    // NO-OP, don't let QueryRunner or JDBI close the connection before the transaction is done
  }

  public void reallyClose() throws SQLException {
    super.close();
  }
}
