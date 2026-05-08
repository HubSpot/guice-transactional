package com.hubspot.guice.transactional;

import com.google.inject.Module;

/**
 * SPI for modules that must be installed ahead of the @Transactional interceptor
 * bindings. Any interceptors bound by these modules will be outermost in the
 * Guice AOP chain.
 *
 * Discovered via {@link java.util.ServiceLoader}.
 * Implementations must have a public no-arg constructor and would typically
 * extend {@link com.google.inject.AbstractModule}.
 */
public interface PreTransactionalModule extends Module {}
