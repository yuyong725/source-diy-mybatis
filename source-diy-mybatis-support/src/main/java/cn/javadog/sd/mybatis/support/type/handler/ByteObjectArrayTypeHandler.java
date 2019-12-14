package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.util.ByteArrayUtils;

/**
 * @author 余勇
 * @date 2019-12-05 14:34
 *
 * Byte[](java) <=> byte[](jdbc)
 */
public class ByteObjectArrayTypeHandler extends BaseTypeHandler<Byte[]> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Byte[] parameter, JdbcType jdbcType) throws SQLException {
    ps.setBytes(i, ByteArrayUtils.convertToPrimitiveArray(parameter));
  }

  @Override
  public Byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
    byte[] bytes = rs.getBytes(columnName);
    return getBytes(bytes);
  }

  @Override
  public Byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    byte[] bytes = rs.getBytes(columnIndex);
    return getBytes(bytes);
  }

  private Byte[] getBytes(byte[] bytes) {
    Byte[] returnValue = null;
    if (bytes != null) {
      returnValue = ByteArrayUtils.convertToObjectArray(bytes);
    }
    return returnValue;
  }

}
