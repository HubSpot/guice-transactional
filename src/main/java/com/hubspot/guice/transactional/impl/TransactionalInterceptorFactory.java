package com.hubspot.guice.transactional.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.aopalliance.intercept.MethodInterceptor;

public class TransactionalInterceptorFactory {

  @SuppressWarnings("unchecked")
  public static Map<Class<? extends Annotation>, MethodInterceptor> instantiateInterceptors() {
    Map<Class<? extends Annotation>, MethodInterceptor> interceptors =
      new LinkedHashMap<>();

    loadClass("javax.transaction.Transactional")
      .ifPresent(clazz ->
        instantiateInterceptor(
          "com.hubspot.guice.transactional.impl.JavaxTransactionalInterceptor"
        )
          .ifPresent(methodInterceptor ->
            interceptors.put((Class<? extends Annotation>) clazz, methodInterceptor)
          )
      );
    loadClass("jakarta.transaction.Transactional")
      .ifPresent(clazz ->
        instantiateInterceptor(
          "com.hubspot.guice.transactional.impl.JakartaTransactionalInterceptor"
        )
          .ifPresent(methodInterceptor ->
            interceptors.put((Class<? extends Annotation>) clazz, methodInterceptor)
          )
      );

    return interceptors;
  }

  private static Optional<Class<?>> loadClass(String fqdn) {
    try {
      return Optional.of(Class.forName(fqdn));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private static Optional<MethodInterceptor> instantiateInterceptor(String fqdn) {
    try {
      Optional<Class<?>> interceptorClass = loadClass(fqdn);

      if (interceptorClass.isPresent()) {
        return Optional.of(
          (MethodInterceptor) interceptorClass
            .get()
            .getDeclaredConstructor()
            .newInstance()
        );
      }

      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
