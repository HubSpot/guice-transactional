package com.hubspot.guice.transactional;

import com.mchange.v2.sql.filter.FilterConnection;
import java.sql.SQLException;

public class TestConnection extends FilterConnection {

  private final String name;

  public TestConnection(String name) {
    this.name = name;
  }

  @Override
  public void setAutoCommit(boolean a) {}

  @Override
  public void commit() {}

  @Override
  public void rollback() {}

  @Override
  public void close() {}

  @Override
  public String getCatalog() throws SQLException {
    return name;
  }
}
