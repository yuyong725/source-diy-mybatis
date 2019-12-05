package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 21:11
 *
 * Integer(java) <=> Integer(jdbc)
 */
public class IntegerTypeHandler extends BaseTypeHandler<Integer> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Integer parameter, JdbcType jdbcType)
      throws SQLException {
    // 直接设置参数即可
    ps.setInt(i, parameter);
  }

  @Override
  public Integer getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    // 获得字段的值
    int result = rs.getInt(columnName);
    // 先通过 rs 判断是否空，如果是空，则返回 null ，否则返回 result
    return (result == 0 && rs.wasNull()) ? null : result;
  }

  @Override
  public Integer getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    // 获得字段的值
    int result = rs.getInt(columnIndex);
    // 先通过 rs 判断是否空，如果是空，则返回 null ，否则返回 result
    return (result == 0 && rs.wasNull()) ? null : result;
  }

}
