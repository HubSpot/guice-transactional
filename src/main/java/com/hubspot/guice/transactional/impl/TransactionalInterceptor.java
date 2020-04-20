package com.hubspot.guice.transactional.impl;

import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.TransactionalException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionalInterceptor implements MethodInterceptor {
  private static final ThreadLocal<TransactionalConnection> TRANSACTION_HOLDER = new ThreadLocal<>();
  private static final Logger LOG = LoggerFactory.getLogger(TransactionalInterceptor.class);
  private static final ThreadLocal<Boolean> IN_TRANSACTION = new ThreadLocal<Boolean>() {

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

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Transactional annotation = invocation.getMethod().getAnnotation(Transactional.class);
    TxType transactionType = annotation.value();

    boolean oldInTransaction = IN_TRANSACTION.get();
    TransactionalConnection oldTransaction = TRANSACTION_HOLDER.get();
    boolean completeTransaction = false;
    LOG.debug("Started invoke chain with transactionType {} old in transaction {} and old transaction {}", transactionType, oldInTransaction, oldInTransaction,
        new RuntimeException("For the stacktrace."));

    if (IN_TRANSACTION.get()) {
      switch (transactionType) {
        case REQUIRES_NEW:
          TRANSACTION_HOLDER.set(null);
          completeTransaction = true;
          break;
        case NOT_SUPPORTED:
          IN_TRANSACTION.set(false);
          TRANSACTION_HOLDER.set(null);
          completeTransaction = true;
          break;
        case NEVER:
          throw new TransactionalException("Transaction is not allowed", new InvalidTransactionException());
      }
    } else {
      switch (transactionType) {
        case REQUIRED:
        case REQUIRES_NEW:
          IN_TRANSACTION.set(true);
          completeTransaction = true;
          break;
        case MANDATORY:
          throw new TransactionalException("Transaction is required", new TransactionRequiredException());
      }
    }

    if (!completeTransaction) {
      LOG.debug("Transaction not completed, continuing.");
      return invocation.proceed();
    } else {
      try {
        LOG.debug("Completing transaction, with current transactional holder {}.", TRANSACTION_HOLDER.get());
        Object returnValue = invocation.proceed();
        TransactionalConnection transaction = TRANSACTION_HOLDER.get();
        if (transaction != null) {
          LOG.debug("Committing transaction.");
          transaction.commit();
        }
        return returnValue;
      } catch (Throwable t) {
        LOG.debug("Exception while completing transaction.", t);
        TransactionalConnection transaction = TRANSACTION_HOLDER.get();
        if (transaction != null) {
          if (shouldRollback(annotation, t)) {
            transaction.rollback();
          } else {
            transaction.commit();
          }
        }
        throw t;
      } finally {
        try {
          TransactionalConnection transaction = TRANSACTION_HOLDER.get();
          if (transaction != null) {
            transaction.reallyClose();
          }
        } finally {
          IN_TRANSACTION.set(oldInTransaction);
          TRANSACTION_HOLDER.set(oldTransaction);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private boolean shouldRollback(Transactional annotation, Throwable t) {
    for (Class dontRollback : annotation.dontRollbackOn()) {
      if (dontRollback.isAssignableFrom(t.getClass())) {
        return false;
      }
    }

    return true;
  }
}
