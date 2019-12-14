package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 21:06
 * Float(java) <=> Float(jdbc)
 */
public class FloatTypeHandler extends BaseTypeHandler<Float> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Float parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setFloat(i, parameter);
  }

  @Override
  public Float getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    float result = rs.getFloat(columnName);
    return (result == 0 && rs.wasNull()) ? null : result;
  }

  @Override
  public Float getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    float result = rs.getFloat(columnIndex);
    return (result == 0 && rs.wasNull()) ? null : result;
  }

}
