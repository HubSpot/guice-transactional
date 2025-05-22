# guice-transactional

## Overview

Miss the convenience of `@Transactional` but have no interest in using JPA? Then guice-transactional is for you.

guice-transactional lets you annotate methods using the standard `@jakarta.transaction.Transactional` annotation and works as you
expect; transactions will be started before the method is called and committed or rolled back after the method completes. It
also implements all of the 
[transaction types](https://jakarta.ee/specifications/transactions/2.0/apidocs/jakarta/transaction/transactional.txtype) for
more complicated use-cases. 

guice-transactional just needs access to the underlying `javax.sql.DataSource` so it will work with any persistence framework,
including jDBI, Hibernate, MyBatis, QueryRunner, or even raw JDBC.

## Maven Dependency

To use on Maven-based projects, add the following dependency:

```xml
<dependency>
  <groupId>com.hubspot.guice</groupId>
  <artifactId>guice-transactional</artifactId>
  <version>0.2.3</version>
</dependency>
```

## Usage

There are a few steps to get up and running with guice-transactional:

1. Install `TransactionalModule` when building your Guice injector, this sets up the necessary method interceptor

2. When configuring your persistence framework, wrap your `DataSource` in a `TransactionalDataSource`

3. Annotate methods with `@jakarta.transaction.Transactional` and profit! The only restriction is that these methods must be on
objects that were created by Guice and they must not have `private` access

## How Does it Work?

When you install `TransactionalModule`, an interceptor is added to methods annotated with `@jakarta.transaction.Transactional`. 
When a transactional method is entered, the interceptor stores this state in a `ThreadLocal`. The `TransactionalDataSource` checks
the interceptor for this flag when a connection is requested. If the flag is set, it disables auto-commit and returns the same 
connection for the duration of the transactional method. Once the transactional method completes, the interceptor either commits or
rolls back the transaction depending on the result of the method call and then clears the thread local state.
