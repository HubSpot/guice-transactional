package com.hubspot.guice.transactional;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.hubspot.guice.transactional.impl.TransactionalInterceptor;
import org.aopalliance.intercept.MethodInterceptor;

public class TransactionalModule extends AbstractModule {

  @Override
  protected void configure() {
    MethodInterceptor transactionalInterceptor = new TransactionalInterceptor();
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transactional.class), transactionalInterceptor);
    binder().requestInjection(transactionalInterceptor);
  }
}
