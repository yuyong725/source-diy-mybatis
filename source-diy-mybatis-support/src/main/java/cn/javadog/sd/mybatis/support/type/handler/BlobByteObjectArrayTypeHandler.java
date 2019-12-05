package cn.javadog.sd.mybatis.support.type.handler;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.util.ByteArrayUtils;

/**
 * @author: 余勇
 * @date: 2019-12-04 23:00
 *
 * Byte[](java) <=> Blob(jdbc)
 */
public class BlobByteObjectArrayTypeHandler extends BaseTypeHandler<Byte[]> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Byte[] parameter, JdbcType jdbcType)
      throws SQLException {
    // 先转成字节数组流
    ByteArrayInputStream bis = new ByteArrayInputStream(ByteArrayUtils.convertToPrimitiveArray(parameter));
    ps.setBinaryStream(i, bis, parameter.length);
  }

  @Override
  public Byte[] getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    // 注意，这里对应的JdbcType是Blob
    Blob blob = rs.getBlob(columnName);
    return getBytes(blob);
  }

  @Override
  public Byte[] getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    Blob blob = rs.getBlob(columnIndex);
    return getBytes(blob);
  }

  /**
   * 将Blob转换成Byte[]
   */
  private Byte[] getBytes(Blob blob) throws SQLException {
    Byte[] returnValue = null;
    if (blob != null) {
      returnValue = ByteArrayUtils.convertToObjectArray(blob.getBytes(1, (int) blob.length()));
    }
    return returnValue;
  }
}
