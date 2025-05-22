package com.hubspot.guice.transactional.impl;

import com.hubspot.guice.transactional.impl.TransactionalAdapter.TxType;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.TransactionRequiredException;
import jakarta.transaction.Transactional;
import jakarta.transaction.TransactionalException;
import org.aopalliance.intercept.MethodInvocation;

public class JakartaTransactionalInterceptor extends AbstractTransactionalInterceptor {

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
