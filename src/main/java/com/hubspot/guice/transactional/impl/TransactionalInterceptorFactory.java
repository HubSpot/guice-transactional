package com.hubspot.guice.transactional.impl;

import com.hubspot.guice.transactional.PreTransactionalModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import org.aopalliance.intercept.MethodInterceptor;

public class TransactionalInterceptorFactory {

  private static final List<PreTransactionalModule> PRE_MODULES;

  static {
    List<PreTransactionalModule> list = new ArrayList<>();
    try {
      ServiceLoader.load(PreTransactionalModule.class).forEach(list::add);
      list.sort(Comparator.comparingInt(PreTransactionalModule::priority));
    } catch (ServiceConfigurationError e) {
      list.clear();
    }
    PRE_MODULES = List.copyOf(list);
  }

  public static List<PreTransactionalModule> preModules() {
    return PRE_MODULES;
  }

  @SuppressWarnings("unchecked")
  public static Set<Class<? extends Annotation>> transactionalAnnotations() {
    Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
    loadClass("javax.transaction.Transactional")
      .ifPresent(c -> annotations.add((Class<? extends Annotation>) c));
    loadClass("jakarta.transaction.Transactional")
      .ifPresent(c -> annotations.add((Class<? extends Annotation>) c));
    return annotations;
  }

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
