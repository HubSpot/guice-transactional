package com.hubspot.guice.transactional;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.hubspot.guice.transactional.impl.TransactionalInterceptor;
import com.hubspot.guice.transactional.impl.TransactionalInterceptorFactory;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestPreModule extends AbstractModule implements PreTransactionalModule {

  static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

  @Override
  protected void configure() {
    for (Class<? extends Annotation> annotation : TransactionalInterceptorFactory.transactionalAnnotations()) {
      bindInterceptor(
        Matchers.any(),
        Matchers.annotatedWith(annotation),
        invocation -> {
          EVENTS.add("pre:enter:inTxn=" + TransactionalInterceptor.inTransaction());
          try {
            return invocation.proceed();
          } finally {
            EVENTS.add("pre:exit:inTxn=" + TransactionalInterceptor.inTransaction());
          }
        }
      );
    }
  }
}
