package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-05 21:43
 *
 * String(java) <=> SQLXML(jdbc)
 * Convert <code>String</code> to/from <code>SQLXML</code>.
 * TODO 貌似很有意思的一个类，难道能拼接mapper里面的xml？
 */
public class SqlxmlTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
      throws SQLException {
    SQLXML sqlxml = ps.getConnection().createSQLXML();
    try {
      sqlxml.setString(parameter);
      ps.setSQLXML(i, sqlxml);
    } finally {
      sqlxml.free();
    }
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return sqlxmlToString(rs.getSQLXML(columnName));
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return sqlxmlToString(rs.getSQLXML(columnIndex));
  }

  protected String sqlxmlToString(SQLXML sqlxml) throws SQLException {
    if (sqlxml == null) {
      return null;
    }
    try {
      return sqlxml.getString();
    } finally {
      sqlxml.free();
    }
  }

}
