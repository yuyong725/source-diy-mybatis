package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 21:37
 * java.sql.Date(java) <=> java.sql.Date(jdbc)
 */
public class SqlDateTypeHandler extends BaseTypeHandler<Date> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setDate(i, parameter);
  }

  @Override
  public Date getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return rs.getDate(columnName);
  }

  @Override
  public Date getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return rs.getDate(columnIndex);
  }

}
