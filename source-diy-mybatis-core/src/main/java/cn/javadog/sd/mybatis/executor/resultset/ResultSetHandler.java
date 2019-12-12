package cn.javadog.sd.mybatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;

/**
 * @author Clinton Begin
 *
 * java.sql.ResultSet 处理器接口
 */
public interface ResultSetHandler {

  /**
   * 处理 {@link java.sql.ResultSet} 成映射的对应的结果
   *
   * @param stmt Statement 对象
   * @param <E> 泛型
   * @return 结果数组
   */
  <E> List<E> handleResultSets(Statement stmt) throws SQLException;

  /**
   * 处理 {@link java.sql.ResultSet} 成 Cursor 对象
   *
   * @param stmt Statement 对象
   * @param <E> 泛型
   * @return Cursor 对象
   */
  <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

  // 暂时忽略，和存储过程相关
  void handleOutputParameters(CallableStatement cs) throws SQLException;

}
