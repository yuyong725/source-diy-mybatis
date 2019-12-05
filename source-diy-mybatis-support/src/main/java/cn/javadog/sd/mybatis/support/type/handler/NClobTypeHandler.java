package cn.javadog.sd.mybatis.support.type.handler;

import java.io.StringReader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 21:20
 * String(java) <=> Clob(jdbc)
 */
public class NClobTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
      throws SQLException {
    StringReader reader = new StringReader(parameter);
    ps.setCharacterStream(i, reader, parameter.length());
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    Clob clob = rs.getClob(columnName);
    return toString(clob);
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    Clob clob = rs.getClob(columnIndex);
    return toString(clob);
  }

  private String toString(Clob clob) throws SQLException {
    return clob == null ? null : clob.getSubString(1, (int) clob.length());
  }

}