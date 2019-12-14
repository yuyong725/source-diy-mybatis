package cn.javadog.sd.mybatis.support.transaction.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.exceptions.TransactionException;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;

/**
 * @author 余勇
 * @date 2019-12-04 13:12
 *
 * 实现 Transaction 接口，基于 JDBC 的事务实现类
 */
public class JdbcTransaction implements Transaction {

  private static final Log log = LogFactory.getLog(JdbcTransaction.class);

  /**
   * Connection 对象
   */
  protected Connection connection;

  /**
   * DataSource 对象
   */
  protected DataSource dataSource;

  /**
   * 事务隔离级别，MYSQL默认级别是可重复读
   * TODO 没有设置值的时候，是什么级别？
   */
  protected TransactionIsolationLevel level;

  /**
   * 是否自动提交，boolean默认值是false
   */
  protected boolean autoCommit;

  /**
   * 构造函数；三个重要参数，数据源，事务级别，是否自动提交
   */
  public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
    dataSource = ds;
    level = desiredLevel;
    autoCommit = desiredAutoCommit;
  }

  /**
   * 构造函数；TODO 参数就连接？
   */
  public JdbcTransaction(Connection connection) {
    this.connection = connection;
  }

  /**
   * 获取连接
   */
  @Override
  public Connection getConnection() throws SQLException {
    if (connection == null) {
      // 没有的话就调用openConnection设置
      openConnection();
    }
    return connection;
  }

  /**
   * 提交事务
   */
  @Override
  public void commit() throws SQLException {
    // 非自动提交，则执行提交事务
    if (connection != null && !connection.getAutoCommit()) {
      if (log.isDebugEnabled()) {
        log.debug("Committing JDBC Connection [" + connection + "]");
      }
      connection.commit();
    }
  }

  /**
   * 回滚事务
   */
  @Override
  public void rollback() throws SQLException {
    // 非自动提交。则回滚事务
    if (connection != null && !connection.getAutoCommit()) {
      if (log.isDebugEnabled()) {
        log.debug("Rolling back JDBC Connection [" + connection + "]");
      }
      connection.rollback();
    }
  }

  /**
   * 关闭连接
   */
  @Override
  public void close() throws SQLException {
    if (connection != null) {
      // 重置连接为自动提交
      resetAutoCommit();
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + connection + "]");
      }
      connection.close();
    }
  }

  /**
   * 设置指定的 autoCommit 属性
   *
   * @param desiredAutoCommit 指定的 autoCommit 属性
   */
  protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
    try {
      // 判断当前连接的级别是否就是要设置的级别
      if (connection.getAutoCommit() != desiredAutoCommit) {
        if (log.isDebugEnabled()) {
          log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
        }
        connection.setAutoCommit(desiredAutoCommit);
      }
    } catch (SQLException e) {
      // 只有实现很垃圾的驱动这里才会报错
      throw new TransactionException("Error configuring AutoCommit.  "
          + "Your driver may not support getAutoCommit() or setAutoCommit(). "
          + "Requested setting: " + desiredAutoCommit + ".  Cause: " + e, e);
    }
  }

  /**
   * 重置 autoCommit 属性为true
   */
  protected void resetAutoCommit() {
    try {
      if (!connection.getAutoCommit()) {
        /**
         * 如果只是单纯的查询，MyBatis不会调用commit/rollback；
         * 一些数据库当进行查询操作时，也会在关闭连接前进行一个commit/rollback的操作；
         * 针对这种情况，我们在关闭连接之前，设置自动提交为true；
         * Sybase数据库在这里就会报错
         */
        if (log.isDebugEnabled()) {
          log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
        }
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Error resetting autocommit to true "
          + "before closing the connection.  Cause: " + e);
      }
    }
  }

  /**
   * 获得 Connection 对象
   *
   * @throws SQLException 获得失败
   */
  protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    // 获得连接
    connection = dataSource.getConnection();
    // 设置隔离级别
    if (level != null) {
      connection.setTransactionIsolation(level.getLevel());
    }
    // 设置 autoCommit 属性
    setDesiredAutoCommit(autoCommit);
  }

  /**
   * 这里没有做实现
   */
  @Override
  public Integer getTimeout() throws SQLException {
    return null;
  }
  
}
