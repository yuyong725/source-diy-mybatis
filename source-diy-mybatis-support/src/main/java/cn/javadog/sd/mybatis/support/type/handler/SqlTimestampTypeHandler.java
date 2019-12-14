package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 21:42
 * Timestamp(java) <=> Timestamp(jdbc)
 */
public class SqlTimestampTypeHandler extends BaseTypeHandler<Timestamp> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Timestamp parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setTimestamp(i, parameter);
  }

  @Override
  public Timestamp getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return rs.getTimestamp(columnName);
  }

  @Override
  public Timestamp getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return rs.getTimestamp(columnIndex);
  }

}
