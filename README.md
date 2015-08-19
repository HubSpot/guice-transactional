# guice-transactional

## Overview

Miss the convenience of `@Transactional` but have no interest in using JPA? Then guice-transactional is for you.

guice-transactional just needs access to the underlying `javax.sql.DataSource` so it will work with any persistence framework,
including jDBI, Hibernate, MyBatis, QueryRunner, or even raw JDBC. 

## Maven Dependency

To use on Maven-based projects, add the following dependency:

```xml
<dependency>
  <groupId>com.hubspot.guice</groupId>
  <artifactId>guice-transactional</artifactId>
  <version>0.2.0</version>
</dependency>
```

## Usage

There are a few steps to get up and running with guice-transactional:

1. Install `TransactionalModule` when building your Guice injector, this sets up the necessary method interceptor

2. Add a Guice binding for `TransactionalDataSource`, wrapping the normal `javax.sql.DataSource`, this might look something like:

  ```java
  @Provides
  @Singleton
  public TransactionalDataSource providesTransactionalDataSource(DataSource dataSource) {
    return new TransactionalDataSource(dataSource);
  }
  ```

3. When configuring your persistence framework, pass the `TransactionalDataSource` from step 2 instead of the undecorated
version (make sure to pass the same instance of `TransactionalDataSource` to your persistence framework that was bound in 
Guice).

4. Annotate methods with `@javax.transaction.Transactional` and profit! The only restriction is that these methods must be on
objects that were created by Guice and they must not be `private`.

## How Does it Work?

When you install `TransactionalModule`, an interceptor is added to methods annotated with `@javax.transaction.Transactional`. 
Before the transactional method is invoked, the interceptor checks out a connection from the data source (which it locates via
the `TransactionalDataSource` Guice binding) and disables autocommit. This connection is stored in a `ThreadLocal` and will be
returned every time someone requests a connection during the transactional method execution. After the transactional method
completes, the transaction will either be committed or rolled back depending on the result of the method call.

## Advanced Configuration

guice-transactional should work out-of-the-box if your application only has a single data source, but if you're connecting to
a few databases or have a sharded setup then you can still use guice-transactional. You will want to bind a custom
implementation of `DataSourceLocator` (in this case you can also skip step 2 from [usage](#usage)). If you have a few 
different data sources, one option is to annotate each transactional method with a second annotation to indicate which data
source it should use, and then in your custom `DataSourceLocator` you can read the value of this annotation and return the
appropriate data source. 

If the data source lookup is dynamic and can't be done using a static annotation (when using a sharded database for example),
your `DataSourceLocator` can inject whatever request-scoped information it needs to determine the appropriate shard and then
return the data source for that shard.
