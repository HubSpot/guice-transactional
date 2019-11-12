package com.hubspot.guice.transactional;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.mchange.v2.sql.filter.FilterConnection;

public class TransactionalDataSourceTest {
  private static TestService testService;
  private static final String OTHER = "other";

  @BeforeClass
  public static void setup() {
    Injector injector = Guice.createInjector(new TransactionalModule(), new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DataSource.class).toInstance(new TransactionalDataSource(new TestDataSource("test")));
        binder.bind(DataSource.class).annotatedWith(Names.named(OTHER)).toInstance(new TransactionalDataSource(new TestDataSource(OTHER)));
        binder.bind(TestService.class);
      }
    });

    testService = injector.getInstance(TestService.class);
  }

  @After
  public void verify() throws SQLException {
    verifyTransactionalStateIsCleared();
  }

  @Test(expected = SQLException.class)
  public void itThrowsOnCrossDBTransactions() throws SQLException {
    testService.multiDbTransaction();
  }

  @Test
  public void itHandlesTransactionsWithNoQueries() throws SQLException {
    testService.transactionWithNoQueries();
  }

  @Test
  public void itHandlesBasicTransaction() throws SQLException {
    List<Connection> connections = testService.transactionalMethod();
    verifySame(connections);
  }

  @Test
  public void itHandlesNestedTransactionWithConnectionCreatedBefore() throws SQLException {
    List<Connection> connections = testService.nestedTransactionalMethodCreateConnectionBefore();
    verifySame(connections);
  }

  @Test
  public void itHandlesNestedTransactionWithConnectionCreatedBeforeAndAfter() throws SQLException {
    List<Connection> connections = testService.nestedTransactionalMethodCreateConnectionBeforeAndAfter();
    verifySame(connections);
  }

  @Test
  public void itHandlesNestedTransactionWithConnectionCreatedAfter() throws SQLException {
    List<Connection> connections = testService.nestedTransactionalMethodCreateConnectionAfter();
    verifySame(connections);
  }

  private void verifyTransactionalStateIsCleared() throws SQLException {
    List<Connection> connections = testService.nonTransactionalMethod();
    assertThat(connections.get(0)).isNotSameAs(connections.get(1));
  }

  private void verifySame(List<Connection> connections) {
    Connection example = connections.get(0);
    for (Connection connection : connections) {
      assertThat(connection).isSameAs(example);
    }
  }

  private static class TestService {
    private final DataSource dataSource;
    private final DataSource otherDataSource;

    @Inject
    public TestService(DataSource dataSource,
                       @Named("other") DataSource otherDataSource) {
      this.dataSource = dataSource;
      this.otherDataSource = otherDataSource;
    }

    public List<Connection> nonTransactionalMethod() throws SQLException {
      return Arrays.asList(dataSource.getConnection(), dataSource.getConnection());
    }

    @Transactional
    public List<Connection> transactionalMethod() throws SQLException {
      return Arrays.asList(dataSource.getConnection(), dataSource.getConnection());
    }

    @Transactional
    public List<Connection> nestedTransactionalMethodCreateConnectionBefore() throws SQLException {
      List<Connection> connections = new ArrayList<>();
      connections.add(dataSource.getConnection());
      connections.add(dataSource.getConnection());
      connections.addAll(transactionalMethod());

      return connections;
    }

    @Transactional
    public void multiDbTransaction() throws SQLException {
      dataSource.getConnection();
      otherDataSource.getConnection();
    }

    @Transactional
    public List<Connection> nestedTransactionalMethodCreateConnectionBeforeAndAfter() throws SQLException {
      List<Connection> connections = new ArrayList<>();
      connections.add(dataSource.getConnection());
      connections.addAll(transactionalMethod());
      connections.add(dataSource.getConnection());

      return connections;
    }

    @Transactional
    public List<Connection> nestedTransactionalMethodCreateConnectionAfter() throws SQLException {
      List<Connection> connections = new ArrayList<>();
      connections.addAll(transactionalMethod());
      connections.add(dataSource.getConnection());
      connections.add(dataSource.getConnection());

      return connections;
    }

    @Transactional
    public List<Connection> transactionWithNoQueries() {
      return Collections.emptyList();
    }
  }

  private static class TestDataSource implements DataSource {
    private final String name;

    public TestDataSource(String name) {
      this.name = name;
    }

    @Override
    public Connection getConnection() throws SQLException {
      return new TestConnection(name);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return new TestConnection(name);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {}

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {}

    @Override
    public int getLoginTimeout() throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException();
    }
  }

  private static class TestConnection extends FilterConnection {
    private final String name;

    public TestConnection(String name) {
      this.name = name;
    }


    @Override
    public void setAutoCommit(boolean a) {}

    @Override
    public void commit() {}

    @Override
    public void rollback() {}

    @Override
    public void close() {}

    @Override
    public String getCatalog() throws SQLException {
      return name;
    }
  }
}
