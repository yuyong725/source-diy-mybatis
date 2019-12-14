package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.OffsetTime;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @since 3.4.5
 * @author Tomas Rohovsky
 */
/**
 * @author 余勇
 * @date 2019-12-05 21:32
 * OffsetTime(java) <=> java.sql.Time(jdbc)
 */
public class OffsetTimeTypeHandler extends BaseTypeHandler<OffsetTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, OffsetTime parameter, JdbcType jdbcType)
          throws SQLException {
    ps.setTime(i, Time.valueOf(parameter.toLocalTime()));
  }

  @Override
  public OffsetTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Time time = rs.getTime(columnName);
    return getOffsetTime(time);
  }

  @Override
  public OffsetTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Time time = rs.getTime(columnIndex);
    return getOffsetTime(time);
  }

  private static OffsetTime getOffsetTime(Time time) {
    if (time != null) {
      return time.toLocalTime().atOffset(OffsetTime.now().getOffset());
    }
    return null;
  }
}
