package cn.javadog.sd.mybatis.support.transaction.managed;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;

/**
 * @author: 余勇
 * @date: 2019-12-04 13:55
 * 实现 Transaction 接口，基于容器管理的事务实现类
 * note 和 JdbcTransaction 相比，少了 autoCommit 属性，空实现 #commit() 和 #rollback() 方法。因此，事务的管理，交给了容器；
 *  可以看看Spring的SpringManagedTransaction，
 */
public class ManagedTransaction implements Transaction {

  private static final Log log = LogFactory.getLog(ManagedTransaction.class);

  /**
   * DataSource 对象
   */
  private DataSource dataSource;

  /**
   * 事务隔离级别
   */
  private TransactionIsolationLevel level;

  /**
   * Connection 对象
   */
  private Connection connection;

  /**
   * 是否关闭连接
   *
   * 这个属性是和 {@link cn.javadog.sd.mybatis.support.transaction.jdbc.JdbcTransaction} 不同的
   */
  private final boolean closeConnection;

  /**
   * 构造
   */
  public ManagedTransaction(Connection connection, boolean closeConnection) {
    this.connection = connection;
    this.closeConnection = closeConnection;
  }

  /**
   * 构造
   */
  public ManagedTransaction(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
    this.dataSource = ds;
    this.level = level;
    this.closeConnection = closeConnection;
  }

  /**
   * 获取连接
   */
  @Override
  public Connection getConnection() throws SQLException {
    // 连接为空，进行创建
    if (this.connection == null) {
      openConnection();
    }
    return this.connection;
  }

  /**
   * 空实现，交给容器
   */
  @Override
  public void commit() throws SQLException {
  }

  /**
   * 空实现，交给容器
   */
  @Override
  public void rollback() throws SQLException {
  }

  /**
   * 关闭连接
   */
  @Override
  public void close() throws SQLException {
    // 如果开启关闭连接功能，则关闭连接
    if (this.closeConnection && this.connection != null) {
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + this.connection + "]");
      }
      this.connection.close();
    }
  }

  /**
   * 获取连接
   */
  protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    // 获得连接
    this.connection = this.dataSource.getConnection();
    // 设置隔离级别
    if (this.level != null) {
      this.connection.setTransactionIsolation(this.level.getLevel());
    }
  }

  /**
   * 不做实现
   */
  @Override
  public Integer getTimeout() throws SQLException {
    return null;
  }

}
