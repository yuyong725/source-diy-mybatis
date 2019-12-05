package cn.javadog.sd.mybatis.support.type.handler;

import java.io.Reader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 20:50
 * Reader(java) <=> Clob(jdbc)
 */
public class ClobReaderTypeHandler extends BaseTypeHandler<Reader> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Reader parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setClob(i, parameter);
  }

  @Override
  public Reader getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return toReader(rs.getClob(columnName));
  }

  @Override
  public Reader getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return toReader(rs.getClob(columnIndex));
  }

  /**
   * Clob => Reader
   */
  private Reader toReader(Clob clob) throws SQLException {
    if (clob == null) {
      return null;
    } else {
      return clob.getCharacterStream();
    }
  }

}
