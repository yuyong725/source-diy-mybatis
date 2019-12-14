package cn.javadog.sd.mybatis.support.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author 余勇
 * @date 2019-12-04 22:15
 *
 * 类型转换处理器
 */
public interface TypeHandler<T> {

  /**
   * 设置 PreparedStatement 的指定参数
   *
   * Java Type => JDBC Type
   *
   * @param ps PreparedStatement 对象
   * @param i 参数占位符的位置
   * @param parameter 参数值
   * @param jdbcType JDBC 类型
   * @throws SQLException 当发生 SQL 异常时
   */
  void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

  /**
   * 获得 ResultSet 的指定字段的值
   *
   * JDBC Type => Java Type
   *
   * @param rs ResultSet 对象
   * @param columnName 字段名
   * @return 值
   * @throws SQLException 当发生 SQL 异常时
   */
  T getResult(ResultSet rs, String columnName) throws SQLException;

  /**
   * 获得 ResultSet 的指定位置字段的值
   *
   * JDBC Type => Java Type
   *
   * @param rs ResultSet 对象
   * @param columnIndex 字段位置
   * @return 值
   * @throws SQLException 当发生 SQL 异常时
   */
  T getResult(ResultSet rs, int columnIndex) throws SQLException;

}
