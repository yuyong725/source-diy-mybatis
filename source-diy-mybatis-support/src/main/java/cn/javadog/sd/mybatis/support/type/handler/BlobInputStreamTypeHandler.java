package cn.javadog.sd.mybatis.support.type.handler;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 14:31
 *
 * InputStream(java) <=> Blob(jdbc)
 */
public class BlobInputStreamTypeHandler extends BaseTypeHandler<InputStream> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, InputStream parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBlob(i, parameter);
  }

  /**
   * Get an {@link InputStream} that corresponds to a specified column name from {@link ResultSet}.
   * @see ResultSet#getBlob(String)
   */
  @Override
  public InputStream getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return toInputStream(rs.getBlob(columnName));
  }

  @Override
  public InputStream getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return toInputStream(rs.getBlob(columnIndex));
  }

  /**
   * Blob => InputStream
   */
  private InputStream toInputStream(Blob blob) throws SQLException {
    if (blob == null) {
      return null;
    } else {
      return blob.getBinaryStream();
    }
  }

}
