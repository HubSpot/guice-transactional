package com.hubspot.guice.transactional.impl;

import static com.hubspot.guice.transactional.impl.TransactionalInterceptor.IN_TRANSACTION;
import static com.hubspot.guice.transactional.impl.TransactionalInterceptor.TRANSACTION_HOLDER;

import com.hubspot.guice.transactional.impl.TransactionalAdapter.TxType;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public abstract class AbstractTransactionalInterceptor implements MethodInterceptor {

  protected abstract TransactionalAdapter getMethodTransactional(
    MethodInvocation invocation
  );

  protected abstract Throwable invalidTransactionException(String message);

  protected abstract Throwable transactionRequiredException(String message);

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    TransactionalAdapter annotation = getMethodTransactional(invocation);
    TxType transactionType = annotation.getTxType();

    boolean oldInTransaction = IN_TRANSACTION.get();
    TransactionalConnection oldTransaction = TRANSACTION_HOLDER.get();
    boolean completeTransaction = false;

    if (IN_TRANSACTION.get()) {
      switch (transactionType) {
        case REQUIRES_NEW:
          TRANSACTION_HOLDER.remove();
          completeTransaction = true;
          break;
        case NOT_SUPPORTED:
          IN_TRANSACTION.set(false);
          TRANSACTION_HOLDER.remove();
          completeTransaction = true;
          break;
        case NEVER:
          throw invalidTransactionException("Transaction is not allowed");
      }
    } else {
      switch (transactionType) {
        case REQUIRED:
        case REQUIRES_NEW:
          IN_TRANSACTION.set(true);
          completeTransaction = true;
          break;
        case MANDATORY:
          throw transactionRequiredException("Transaction is required");
      }
    }

    if (!completeTransaction) {
      return invocation.proceed();
    } else {
      try {
        Object returnValue = invocation.proceed();
        TransactionalConnection transaction = TRANSACTION_HOLDER.get();
        if (transaction != null) {
          transaction.commit();
        }
        return returnValue;
      } catch (Throwable t) {
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

  private boolean shouldRollback(TransactionalAdapter annotation, Throwable t) {
    for (Class<?> dontRollback : annotation.getDontRollbackOn()) {
      if (dontRollback.isAssignableFrom(t.getClass())) {
        return false;
      }
    }

    return true;
  }
}
