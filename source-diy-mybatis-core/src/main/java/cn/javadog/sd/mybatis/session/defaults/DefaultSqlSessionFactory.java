package cn.javadog.sd.mybatis.session.defaults;

import java.sql.Connection;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.executor.ErrorContext;
import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.mapping.Environment;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.transaction.TransactionFactory;
import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;
import cn.javadog.sd.mybatis.support.transaction.managed.ManagedTransactionFactory;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;


/**
 * @author Clinton Begin
 *
 * 默认的 SqlSessionFactory 实现类
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

  private final Configuration configuration;

  public DefaultSqlSessionFactory(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public SqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }

  @Override
  public SqlSession openSession(boolean autoCommit) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
  }

  @Override
  public SqlSession openSession(ExecutorType execType) {
    return openSessionFromDataSource(execType, null, false);
  }

  @Override
  public SqlSession openSession(TransactionIsolationLevel level) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return openSessionFromDataSource(execType, level, false);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return openSessionFromDataSource(execType, null, autoCommit);
  }

  @Override
  public SqlSession openSession(Connection connection) {
    return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return openSessionFromConnection(execType, connection);
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * 获得 SqlSession 对象
   */
  private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      // 获得 Environment 对象
      final Environment environment = configuration.getEnvironment();
      // 创建 Transaction 对象
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      // 创建 Executor 对象
      final Executor executor = configuration.newExecutor(tx, execType);
      // 创建 DefaultSqlSession 对象
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      // 如果发生异常，则关闭 Transaction 对象
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionUtil.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 获得 SqlSession 对象
   */
  private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
    try {
      // 获得是否可以自动提交
      boolean autoCommit;
      try {
        autoCommit = connection.getAutoCommit();
      } catch (SQLException e) {
        // Failover to true, as most poor drivers
        // or databases won't support transactions
        autoCommit = true;
      }
      // 获得 Environment 对象
      final Environment environment = configuration.getEnvironment();
      // 创建 Transaction 对象
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      final Transaction tx = transactionFactory.newTransaction(connection);
      // 创建 Executor 对象
      final Executor executor = configuration.newExecutor(tx, execType);
      // 创建 DefaultSqlSession 对象
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 获得 TransactionFactory 对象
   */
  private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
    // 情况一，创建 ManagedTransactionFactory 对象
    if (environment == null || environment.getTransactionFactory() == null) {
      return new ManagedTransactionFactory();
    }
    // 情况二，使用 `environment` 中的
    return environment.getTransactionFactory();
  }

  private void closeTransaction(Transaction tx) {
    if (tx != null) {
      try {
        tx.close();
      } catch (SQLException ignore) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

}
