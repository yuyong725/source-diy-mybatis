package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @since 3.4.5
 * @author Tomas Rohovsky
 */
/**
 * @author: 余勇
 * @date: 2019-12-05 21:14
 * LocalDate(java) <=> java.sql.Date(jdbc)
 */
public class LocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
          throws SQLException {
    ps.setDate(i, Date.valueOf(parameter));
  }

  @Override
  public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Date date = rs.getDate(columnName);
    return getLocalDate(date);
  }

  @Override
  public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Date date = rs.getDate(columnIndex);
    return getLocalDate(date);
  }

  private static LocalDate getLocalDate(Date date) {
    if (date != null) {
      return date.toLocalDate();
    }
    return null;
  }
}
