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
 * @author 余勇
 * @date 2019-12-17 16:39
 *
 * 默认的 SqlSessionFactory 实现类
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * 构造函数
   */
  public DefaultSqlSessionFactory(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(boolean autoCommit) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType) {
    return openSessionFromDataSource(execType, null, false);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(TransactionIsolationLevel level) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return openSessionFromDataSource(execType, level, false);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return openSessionFromDataSource(execType, null, autoCommit);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(Connection connection) {
    return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return openSessionFromConnection(execType, connection);
  }

  /**
   * 获取 configuration
   */
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
      // 开启一个事务
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      // 创建 Executor 对象
      final Executor executor = configuration.newExecutor(tx, execType);
      // 创建 DefaultSqlSession 对象
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      // 如果发生异常，则关闭 Transaction 对象
      closeTransaction(tx);
      throw ExceptionUtil.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 通过 connection 获得 SqlSession 对象
   */
  private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
    try {
      // 获得是否可以自动提交
      boolean autoCommit;
      try {
        autoCommit = connection.getAutoCommit();
      } catch (SQLException e) {
        // 出错直接设置为true，因为大多数糟糕的驱动或者数据库，不支持数据库事务
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

  /**
   * 关闭事务
   */
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
