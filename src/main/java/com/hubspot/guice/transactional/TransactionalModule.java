package com.hubspot.guice.transactional;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.hubspot.guice.transactional.impl.TransactionalInterceptorFactory;
import java.lang.annotation.Annotation;
import java.util.Map.Entry;
import org.aopalliance.intercept.MethodInterceptor;

public class TransactionalModule extends AbstractModule {

  @Override
  protected void configure() {
    for (Entry<Class<? extends Annotation>, MethodInterceptor> entry : TransactionalInterceptorFactory
      .instantiateInterceptors()
      .entrySet()) {
      bindInterceptor(
        Matchers.any(),
        Matchers.annotatedWith(entry.getKey()),
        entry.getValue()
      );
    }
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
