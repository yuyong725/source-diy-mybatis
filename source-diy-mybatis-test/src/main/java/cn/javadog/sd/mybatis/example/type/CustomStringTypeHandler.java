package cn.javadog.sd.mybatis.example.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;


public class CustomStringTypeHandler implements TypeHandler<String> {

  @Override
  public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, parameter);
  }

  @Override
  public String getResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getString(columnName);
  }

  @Override
  public String getResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getString(columnIndex);
  }

}
