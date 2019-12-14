package cn.javadog.sd.mybatis.support.type.handler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-04 22:59
 *
 * BigInteger(java) <=> BigInteger(jdbc)
 */
public class BigIntegerTypeHandler extends BaseTypeHandler<BigInteger> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, BigInteger parameter, JdbcType jdbcType) throws SQLException {
    ps.setBigDecimal(i, new BigDecimal(parameter));
  }

  @Override
  public BigInteger getNullableResult(ResultSet rs, String columnName) throws SQLException {
    BigDecimal bigDecimal = rs.getBigDecimal(columnName);
    return bigDecimal == null ? null : bigDecimal.toBigInteger();
  }

  @Override
  public BigInteger getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    BigDecimal bigDecimal = rs.getBigDecimal(columnIndex);
    return bigDecimal == null ? null : bigDecimal.toBigInteger();
  }

}
