package com.hubspot.guice.transactional;

import com.google.inject.ImplementedBy;
import com.hubspot.guice.transactional.impl.DefaultDataSourceLocator;
import org.aopalliance.intercept.MethodInvocation;

@ImplementedBy(DefaultDataSourceLocator.class)
public interface DataSourceLocator {
  TransactionalDataSource locate(MethodInvocation methodInvocation);
}
