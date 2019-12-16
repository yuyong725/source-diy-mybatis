package cn.javadog.sd.mybatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;

/**
 * @author Clinton Begin
 *
 *
 *
 */
/**
 * @author 余勇
 * @date 2019-12-16 20:28
 * Statement 处理器，
 * 其中 Statement 包含 java.sql.Statement、java.sql.PreparedStatement、java.sql.CallableStatement 三种
 */
public interface StatementHandler {

  /**
   * 准备操作，可以理解成创建 Statement 对象
   *
   * @param connection         Connection 对象
   * @param transactionTimeout 事务超时时间
   * @return Statement 对象
   */
  Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException;

  /**
   * 设置 Statement 对象的参数
   *
   * @param statement Statement 对象
   */
  void parameterize(Statement statement) throws SQLException;

  /**
   * 添加 Statement 对象的批量操作
   *
   * @param statement Statement 对象
   */
  void batch(Statement statement) throws SQLException;

  /**
   * 执行写操作，增改删都是写
   *
   * @param statement Statement 对象
   * @return 影响的条数
   */
  int update(Statement statement) throws SQLException;

  /**
   * 执行读操作
   *
   * @param statement Statement 对象
   * @param resultHandler ResultHandler 对象，处理结果
   * @param <E> 泛型
   * @return 读取的结果
   */
  <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;

  /**
   * 执行读操作，返回 Cursor 对象
   *
   * @param statement Statement 对象
   * @param <E> 泛型
   * @return Cursor 对象
   */
  <E> Cursor<E> queryCursor(Statement statement) throws SQLException;

  /**
   * 获取 BoundSql 对象
   */
  BoundSql getBoundSql();

  /**
   * 获取 ParameterHandler 对象
   */
  ParameterHandler getParameterHandler();

}
