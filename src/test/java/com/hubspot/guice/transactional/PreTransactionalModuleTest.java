package com.hubspot.guice.transactional;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.hubspot.guice.transactional.impl.TransactionalInterceptorFactory;
import com.mchange.v2.sql.filter.FilterConnection;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PreTransactionalModuleTest {

  private static TestService testService;

  @BeforeClass
  public static void setup() {
    Injector injector = Guice.createInjector(
      new TransactionalModule(),
      new Module() {
        @Override
        public void configure(Binder binder) {
          binder
            .bind(DataSource.class)
            .toInstance(new TransactionalDataSource(new StubDataSource()));
          binder.bind(TestService.class);
        }
      }
    );
    testService = injector.getInstance(TestService.class);
  }

  @Before
  public void clearEvents() {
    TestPreModule.EVENTS.clear();
  }

  @Test
  public void itDiscoversPeModulesViaServiceLoader() {
    assertThat(TransactionalInterceptorFactory.preTransactionalModules()).hasSize(1);
    assertThat(TransactionalInterceptorFactory.preTransactionalModules().get(0))
      .isInstanceOf(TestPreModule.class);
  }

  @Test
  public void itBindsPreModuleInterceptorOutermost() throws SQLException {
    testService.doWork();

    assertThat(TestPreModule.EVENTS)
      .containsExactly("pre:enter:inTxn=false", "pre:exit:inTxn=false");
  }

  @Test
  public void itBindsPreModuleInterceptorOutermostForNestedTransactions()
    throws SQLException {
    testService.outerWork();

    assertThat(TestPreModule.EVENTS)
      .containsExactly(
        "pre:enter:inTxn=false",
        "pre:enter:inTxn=true",
        "pre:exit:inTxn=true",
        "pre:exit:inTxn=false"
      );
  }

  @Test
  public void itExposesTransactionalAnnotations() {
    assertThat(TransactionalInterceptorFactory.transactionalAnnotations())
      .contains(Transactional.class);
  }

  static class TestService {

    private final DataSource dataSource;

    @Inject
    public TestService(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Transactional
    public void doWork() throws SQLException {
      dataSource.getConnection();
    }

    @Transactional
    public void outerWork() throws SQLException {
      dataSource.getConnection();
      doWork();
    }
  }

  static class StubDataSource implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
      return new StubConnection();
    }

    @Override
    public Connection getConnection(String u, String p) throws SQLException {
      return new StubConnection();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }

    @Override
    public PrintWriter getLogWriter() {
      return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {}

    @Override
    public void setLoginTimeout(int seconds) {}

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException();
    }
  }

  static class StubConnection extends FilterConnection {

    @Override
    public void setAutoCommit(boolean a) {}

    @Override
    public void commit() {}

    @Override
    public void rollback() {}

    @Override
    public void close() {}

    @Override
    public String getCatalog() {
      return "test";
    }
  }
}
