package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 21:58
 * ZonedDateTime(java) <=> Timestamp(jdbc)
 */
public class ZonedDateTimeTypeHandler extends BaseTypeHandler<ZonedDateTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, ZonedDateTime parameter, JdbcType jdbcType)
          throws SQLException {
    ps.setTimestamp(i, Timestamp.from(parameter.toInstant()));
  }

  @Override
  public ZonedDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName);
    return getZonedDateTime(timestamp);
  }

  @Override
  public ZonedDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex);
    return getZonedDateTime(timestamp);
  }

   private static ZonedDateTime getZonedDateTime(Timestamp timestamp) {
    if (timestamp != null) {
      return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }
    return null;
  }
}
