package com.hubspot.guice.transactional.impl;

import com.hubspot.guice.transactional.impl.TransactionalAdapter.TxType;
import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.Transactional;
import javax.transaction.TransactionalException;
import org.aopalliance.intercept.MethodInvocation;

public class JavaxTransactionalInterceptor extends AbstractTransactionalInterceptor {

  @Override
  protected TransactionalAdapter getMethodTransactional(MethodInvocation invocation) {
    Transactional annotation = invocation.getMethod().getAnnotation(Transactional.class);
    return new TransactionalAdapter(
      TxType.valueOf(annotation.value().name()),
      annotation.rollbackOn(),
      annotation.dontRollbackOn()
    );
  }

  @Override
  protected Throwable invalidTransactionException(String message) {
    return new TransactionalException(message, new InvalidTransactionException());
  }

  @Override
  protected Throwable transactionRequiredException(String message) {
    return new TransactionalException(message, new TransactionRequiredException());
  }
}
