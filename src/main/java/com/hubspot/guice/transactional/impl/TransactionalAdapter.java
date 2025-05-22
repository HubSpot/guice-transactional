package com.hubspot.guice.transactional.impl;

public class TransactionalAdapter {

  public TxType getTxType() {
    return txType;
  }

  public Class[] getRollbackOn() {
    return rollbackOn;
  }

  public Class[] getDontRollbackOn() {
    return dontRollbackOn;
  }

  private final TxType txType;

  private final Class[] rollbackOn;

  private final Class[] dontRollbackOn;

  public TransactionalAdapter(TxType txType, Class[] rollbackOn, Class[] dontRollbackOn) {
    this.txType = txType;
    this.rollbackOn = rollbackOn;
    this.dontRollbackOn = dontRollbackOn;
  }

  public enum TxType {
    REQUIRED,
    REQUIRES_NEW,
    MANDATORY,
    SUPPORTS,
    NOT_SUPPORTED,
    NEVER,
  }
}
