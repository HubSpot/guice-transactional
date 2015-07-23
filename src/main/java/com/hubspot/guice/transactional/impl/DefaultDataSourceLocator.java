package com.hubspot.guice.transactional.impl;

import com.google.inject.Inject;
import com.hubspot.guice.transactional.DataSourceLocator;
import com.hubspot.guice.transactional.TransactionalDataSource;
import org.aopalliance.intercept.MethodInvocation;

public class DefaultDataSourceLocator implements DataSourceLocator {
  private final TransactionalDataSource dataSource;

  @Inject
  public DefaultDataSourceLocator(TransactionalDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public TransactionalDataSource locate(MethodInvocation methodInvocation) {
    return dataSource;
  }
}
