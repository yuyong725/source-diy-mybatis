package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 14:33
 *
 * byte[](java) <=> byte[](jdbc)
 * TODO byte[]默认的是哪个呢？ {@link BlobTypeHandler} 也能转
 */
public class ByteArrayTypeHandler extends BaseTypeHandler<byte[]> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, byte[] parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBytes(i, parameter);
  }

  @Override
  public byte[] getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return rs.getBytes(columnName);
  }

  @Override
  public byte[] getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return rs.getBytes(columnIndex);
  }

}
