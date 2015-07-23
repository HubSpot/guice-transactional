package com.hubspot.guice.transactional.impl;

import com.hubspot.guice.transactional.DataSourceLocator;
import com.hubspot.guice.transactional.Transactional;
import com.hubspot.guice.transactional.TransactionalDataSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Inject;

public class TransactionalInterceptor implements MethodInterceptor {

  @Inject
  DataSourceLocator dataSourceLocator;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    TransactionalDataSource dataSource = dataSourceLocator.locate(invocation);

    if (enabled(invocation)) {
      return runWithTransaction(invocation, dataSource);
    } else {
      return runWithoutTransaction(invocation, dataSource);
    }
  }

  private Object runWithTransaction(MethodInvocation invocation, TransactionalDataSource dataSource) throws Throwable {
    if (dataSource.inTransaction()) {
      return invocation.proceed();
    } else {
      dataSource.startTransaction();
      try {
        Object returnValue = invocation.proceed();
        dataSource.commitTransaction();
        return returnValue;
      } catch (Throwable t) {
        dataSource.rollbackTransaction();
        throw t;
      } finally {
        dataSource.endTransaction();
      }
    }
  }

  private Object runWithoutTransaction(MethodInvocation invocation, TransactionalDataSource dataSource) throws Throwable {
    TransactionalConnection transaction = dataSource.pauseTransaction();
    try {
      return invocation.proceed();
    } finally {
      dataSource.resumeTransaction(transaction);
    }
  }

  private boolean enabled(MethodInvocation invocation) {
    return invocation.getMethod().getAnnotation(Transactional.class).value();
  }
}
