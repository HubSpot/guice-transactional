package com.hubspot.guice.transactional.impl;

import com.hubspot.guice.transactional.DataSourceLocator;
import com.hubspot.guice.transactional.TransactionalDataSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Inject;
import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.TransactionalException;

public class TransactionalInterceptor implements MethodInterceptor {

  @Inject
  DataSourceLocator dataSourceLocator;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    TransactionalDataSource dataSource = dataSourceLocator.locate(invocation);

    Transactional annotation = invocation.getMethod().getAnnotation(Transactional.class);
    TxType transactionType = annotation.value();

    TransactionalConnection oldTransaction = dataSource.getTransaction();
    boolean completeTransaction = false;

    if (dataSource.inTransaction()) {
      switch (transactionType) {
        case REQUIRES_NEW:
          oldTransaction = dataSource.pauseTransaction();
          dataSource.startTransaction();
          completeTransaction = true;
          break;
        case NOT_SUPPORTED:
          oldTransaction = dataSource.pauseTransaction();
          break;
        case NEVER:
          throw new TransactionalException("Transaction is not allowed", new InvalidTransactionException());
      }
    } else {
      switch (transactionType) {
        case REQUIRED:
        case REQUIRES_NEW:
          dataSource.startTransaction();
          completeTransaction = true;
          break;
        case MANDATORY:
          throw new TransactionalException("Transaction is required", new TransactionRequiredException());
      }
    }

    try {
      Object returnValue = invocation.proceed();
      if (completeTransaction) {
        dataSource.commitTransaction();
      }
      return returnValue;
    } catch (Throwable t) {
      if (completeTransaction) {
        if (shouldRollback(annotation, t)) {
          dataSource.rollbackTransaction();
        } else {
          dataSource.commitTransaction();
        }
      }
      throw t;
    } finally {
      try {
        if (completeTransaction) {
          dataSource.endTransaction();
        }
      } finally {
        dataSource.resumeTransaction(oldTransaction);
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
