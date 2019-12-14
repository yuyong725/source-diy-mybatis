package cn.javadog.sd.mybatis.support.transaction;

import java.sql.Connection;

/**
 * @author 余勇
 * @date 2019-12-04 13:16
 * 事务的隔离级别，源码是在session包里面，这里移到了事务
 * 参考文章：https://tech.meituan.com/2014/08/20/innodb-lock.html
 */
public enum TransactionIsolationLevel {
  /**
   * 没有事务，也就是自动提交
   */
  NONE(Connection.TRANSACTION_NONE),

  /**
   * 读已提交
   */
  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

  /**
   * 读未提交
   */
  READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),

  /**
   * 可重复读
   */
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

  /**
   * 序列化
   */
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

  private final int level;

  private TransactionIsolationLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }
}
