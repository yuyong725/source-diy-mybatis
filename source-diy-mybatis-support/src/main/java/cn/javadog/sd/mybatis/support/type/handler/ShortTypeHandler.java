package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 21:36
 * Short(java) <=> Short(jdbc)
 */
public class ShortTypeHandler extends BaseTypeHandler<Short> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Short parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setShort(i, parameter);
  }

  @Override
  public Short getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    short result = rs.getShort(columnName);
    return (result == 0 && rs.wasNull()) ? null : result;
  }

  @Override
  public Short getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    short result = rs.getShort(columnIndex);
    return (result == 0 && rs.wasNull()) ? null : result;
  }

}
