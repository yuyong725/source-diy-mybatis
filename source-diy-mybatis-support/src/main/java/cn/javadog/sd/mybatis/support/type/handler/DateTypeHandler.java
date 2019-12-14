package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 20:59
 *
 * java.util.Date(java) <=> java.sql.Timestamp(jdbc)
 */
public class DateTypeHandler extends BaseTypeHandler<Date> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType)
      throws SQLException {
    // 将 Date 转换成 Timestamp 类型
    // 然后设置到 ps 中
    ps.setTimestamp(i, new Timestamp(parameter.getTime()));
  }

  @Override
  public Date getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    // 获得 Timestamp 的值
    Timestamp sqlTimestamp = rs.getTimestamp(columnName);
    // 将 Timestamp 转换成 Date 类型
    if (sqlTimestamp != null) {
      return new Date(sqlTimestamp.getTime());
    }
    return null;
  }

  @Override
  public Date getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    // 获得 Timestamp 的值
    Timestamp sqlTimestamp = rs.getTimestamp(columnIndex);
    // 将 Timestamp 转换成 Date 类型
    if (sqlTimestamp != null) {
      return new Date(sqlTimestamp.getTime());
    }
    return null;
  }

}
