package cn.javadog.sd.mybatis.support.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author 余勇
 * @date 2019-12-04 13:10
 * 事务接口,管理实务的生命周期，包括：创建，准备，提交，回滚，关闭
 * note 与Spring结合后使用的是 SpringManagedTransaction
 */
public interface Transaction {

  /**
   * 获得连接
   */
  Connection getConnection() throws SQLException;

  /**
   * 事务提交
   */
  void commit() throws SQLException;

  /**
   * 事务回滚
   */
  void rollback() throws SQLException;

  /**
   * 关闭连接
   */
  void close() throws SQLException;

  /**
   * 获得事务超时时间，Integer而不是int，允许子类不实现，因为int是不能为null的
   */
  Integer getTimeout() throws SQLException;
  
}
