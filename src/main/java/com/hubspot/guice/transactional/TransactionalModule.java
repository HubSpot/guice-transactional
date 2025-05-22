package com.hubspot.guice.transactional;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.hubspot.guice.transactional.impl.TransactionalInterceptor;
import jakarta.transaction.Transactional;

public class TransactionalModule extends AbstractModule {

  @Override
  protected void configure() {
    bindInterceptor(
      Matchers.any(),
      Matchers.annotatedWith(Transactional.class),
      new TransactionalInterceptor()
    );
  }

  @Override
  public boolean equals(Object o) {
    return o != null && o.getClass() == getClass();
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
