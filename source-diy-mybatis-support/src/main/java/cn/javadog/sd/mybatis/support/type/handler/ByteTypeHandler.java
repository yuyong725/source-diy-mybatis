package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 20:45
 * Byte(java) <=> Byte(jdbc)
 */
public class ByteTypeHandler extends BaseTypeHandler<Byte> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Byte parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setByte(i, parameter);
  }

  @Override
  public Byte getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    byte result = rs.getByte(columnName);
    return (result == 0 && rs.wasNull()) ? null : result;
  }

  @Override
  public Byte getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    byte result = rs.getByte(columnIndex);
    return (result == 0 && rs.wasNull()) ? null : result;
  }

}
