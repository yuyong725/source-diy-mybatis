package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 21:53
 *
 * YearMonth(java) <=> String(jdbc)
 * 这个类的实现注意依赖于 {@link YearMonth#parse(CharSequence)} 方法，因此它要求数据库对应字段的格式必须是 "yyyy-MM"，比如 "2016-08"
 */
public class YearMonthTypeHandler extends BaseTypeHandler<YearMonth> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, YearMonth yearMonth, JdbcType jt) throws SQLException {
    ps.setString(i, yearMonth.toString());
  }

  @Override
  public YearMonth getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String value = rs.getString(columnName);
    return value == null ? null : YearMonth.parse(value);
  }

  @Override
  public YearMonth getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String value = rs.getString(columnIndex);
    return value == null ? null : YearMonth.parse(value);
  }

}
