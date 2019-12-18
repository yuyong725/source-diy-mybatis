package cn.javadog.sd.mybatis.session;

import java.sql.Connection;

import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;

/**
 * Creates an {@link SqlSession} out of a connection or a DataSource
 * 
 * @author Clinton Begin
 */
/**
 * @author 余勇
 * @date 2019-12-17 15:53
 *
 * 会话工厂
 * 通过connection 或 DataSource 开启一个{@link SqlSession}
 */
public interface SqlSessionFactory {

  /**
   * 开启会话
   */
  SqlSession openSession();

  /**
   * 开启会话，允许设置自动提交属性
   */
  SqlSession openSession(boolean autoCommit);

  /**
   * 开启指定连接的会话
   */
  SqlSession openSession(Connection connection);

  /**
   * 开启指定事务级别的会话
   */
  SqlSession openSession(TransactionIsolationLevel level);

  /**
   * 开启使用指定执行器的会话
   */
  SqlSession openSession(ExecutorType execType);

  /**
   * 开启会话，指定执行器类型和自动提交属性
   */
  SqlSession openSession(ExecutorType execType, boolean autoCommit);

  /**
   * 开启会话，指定执行器级别和事务隔离级别
   */
  SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);

  /**
   * 开启会话，指定执行器级别
   */
  SqlSession openSession(ExecutorType execType, Connection connection);

  /**
   * 获取 Configuration
   */
  Configuration getConfiguration();

}
