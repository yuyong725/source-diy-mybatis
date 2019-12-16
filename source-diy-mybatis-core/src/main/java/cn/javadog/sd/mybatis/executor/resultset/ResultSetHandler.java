package cn.javadog.sd.mybatis.executor.resultset;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;

/**
 * @author 余勇
 * @date 2019-12-15 17:39
 *
 * java.sql.ResultSet 处理器接口。删除了存储过程相关的方法
 */
public interface ResultSetHandler {

  /**
   * 将 {@link java.sql.ResultSet} 映射到对应的结果
   *
   * @param stmt Statement 对象
   * @param <E> 泛型
   * @return 结果数组
   */
  <E> List<E> handleResultSets(Statement stmt) throws SQLException;

  /**
   * 将 {@link java.sql.ResultSet} 映射到 Cursor 对象
   *
   * @param stmt Statement 对象
   * @param <E> 泛型
   * @return Cursor 对象
   */
  <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

}
