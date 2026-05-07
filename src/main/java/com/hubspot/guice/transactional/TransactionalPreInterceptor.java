package com.hubspot.guice.transactional;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * SPI for interceptors that should be bound ahead of the @Transactional interceptor
 * in the Guice AOP chain (i.e., outermost).
 *
 * Discovered via {@link java.util.ServiceLoader} at class-load time.
 * Implementations must have a public no-arg constructor.
 */
public interface TransactionalPreInterceptor extends MethodInterceptor {
  default int priority() {
    return 0;
  }
}
