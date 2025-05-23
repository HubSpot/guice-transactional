package com.hubspot.guice.transactional.impl;

public class TransactionalInterceptor {

  protected static final ThreadLocal<TransactionalConnection> TRANSACTION_HOLDER =
    new ThreadLocal<>();
  protected static final ThreadLocal<Boolean> IN_TRANSACTION =
    new ThreadLocal<Boolean>() {
      @Override
      protected Boolean initialValue() {
        return false;
      }
    };

  public static boolean inTransaction() {
    return IN_TRANSACTION.get();
  }

  public static TransactionalConnection getTransaction() {
    return TRANSACTION_HOLDER.get();
  }

  public static void setTransaction(TransactionalConnection transaction) {
    TRANSACTION_HOLDER.set(transaction);
  }
}
