package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 21:31
 * Object(java) <=> Object(jdbc)
 */
public class ObjectTypeHandler extends BaseTypeHandler<Object> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setObject(i, parameter);
  }

  @Override
  public Object getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return rs.getObject(columnName);
  }

  @Override
  public Object getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return rs.getObject(columnIndex);
  }

}
