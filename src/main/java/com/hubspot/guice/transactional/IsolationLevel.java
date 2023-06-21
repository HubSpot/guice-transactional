package com.hubspot.guice.transactional;

import java.sql.Connection;

public enum IsolationLevel {
  DEFAULT(-1),
  NONE(Connection.TRANSACTION_NONE),
  READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

  private final int value;

  IsolationLevel(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
