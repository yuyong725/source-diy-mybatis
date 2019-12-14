package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 20:57
 *
 * java.util.Date(java) <=> java.sql.Date(jdbc)
 * 数据库里的时间有多种类型，以 MySQL 举例子，有 date、timestamp、datetime 三种类型
 */
public class DateOnlyTypeHandler extends BaseTypeHandler<Date> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType)
      throws SQLException {
    // 将 java Date 转换成 sql Date 类型
    ps.setDate(i, new java.sql.Date(parameter.getTime()));
  }

  @Override
  public Date getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    // 获得 sql Date 的值
    java.sql.Date sqlDate = rs.getDate(columnName);
    // 将 sql Date 转换成 java Date 类型
    if (sqlDate != null) {
      return new Date(sqlDate.getTime());
    }
    return null;
  }

  @Override
  public Date getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    // 获得 sql Date 的值
    java.sql.Date sqlDate = rs.getDate(columnIndex);
    // 将 sql Date 转换成 java Date 类型
    if (sqlDate != null) {
      return new Date(sqlDate.getTime());
    }
    return null;
  }

}
