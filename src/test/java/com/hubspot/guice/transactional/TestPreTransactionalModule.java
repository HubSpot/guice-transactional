package com.hubspot.guice.transactional;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.hubspot.guice.transactional.impl.TransactionalInterceptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.transaction.Transactional;

public class TestPreTransactionalModule extends AbstractModule implements PreTransactionalModule {

  static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

  @Override
  protected void configure() {
    bindInterceptor(
      Matchers.any(),
      Matchers.annotatedWith(Transactional.class),
      invocation -> {
        EVENTS.add("pre:enter:txn=" + TransactionalInterceptor.inTransaction());
        try {
          return invocation.proceed();
        } finally {
          EVENTS.add("pre:exit:txn=" + TransactionalInterceptor.inTransaction());
        }
      }
    );

    }

  }
