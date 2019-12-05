package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 21:43
 * java.sql.Time(java) <=> java.sql.Time(jdbc)
 */
public class SqlTimeTypeHandler extends BaseTypeHandler<Time> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Time parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setTime(i, parameter);
  }

  @Override
  public Time getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return rs.getTime(columnName);
  }

  @Override
  public Time getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return rs.getTime(columnIndex);
  }

}
