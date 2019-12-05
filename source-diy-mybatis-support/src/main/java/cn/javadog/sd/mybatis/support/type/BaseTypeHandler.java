package cn.javadog.sd.mybatis.support.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.exceptions.ResultMapException;
import cn.javadog.sd.mybatis.support.exceptions.TypeException;

/**
 * @author: 余勇
 * @date: 2019-12-04 22:34
 *
 * 实现 TypeHandler 接口，继承 TypeReference 抽象类，TypeHandler 基础抽象类。
 * 关于TypeReference简单了解一下：https://blog.csdn.net/zhuzj12345/article/details/102914545，反射模块讲解Type比较多。
 *
 * 3.5.0版本之后，null值的处理由子类完成，不再直接调用{@link ResultSet#wasNull()}和 {@link CallableStatement#wasNull()}去处理 SQL的null
 */
public abstract class BaseTypeHandler<T> extends TypeReference<T> implements TypeHandler<T> {

  /**
   * 设置 PreparedStatement 的指定参数
   */
  @Override
  public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
    // 参数为空时，设置为 null 类型
    if (parameter == null) {
      if (jdbcType == null) {
        // note 这段代码解释了，为什么SQL中的参数如果为空，必须指定JDBC类型
        throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
      }
      try {
        // 设置指定位置为TYPE_CODE对应的类型的null
        ps.setNull(i, jdbcType.TYPE_CODE);
      } catch (SQLException e) {
        throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . " +
                "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. " +
                "Cause: " + e, e);
      }

    } else {
      try {
        // 参数非空时，设置对应的参数
        setNonNullParameter(ps, i, parameter, jdbcType);
      } catch (Exception e) {
        throw new TypeException("Error setting non null for parameter #" + i + " with JdbcType " + jdbcType + " . " +
                "Try setting a different JdbcType for this parameter or a different configuration property. " +
                "Cause: " + e, e);
      }
    }
  }

  /**
   * 获得 ResultSet 的指定字段的值
   */
  @Override
  public T getResult(ResultSet rs, String columnName) throws SQLException {
    try {
      return getNullableResult(rs, columnName);
    } catch (Exception e) {
      throw new ResultMapException("Error attempting to get column '" + columnName + "' from result set.  Cause: " + e, e);
    }
  }

  /**
   * 获得 ResultSet 的指定位置字段的值
   */
  @Override
  public T getResult(ResultSet rs, int columnIndex) throws SQLException {
    try {
      return getNullableResult(rs, columnIndex);
    } catch (Exception e) {
      throw new ResultMapException("Error attempting to get column #" + columnIndex+ " from result set.  Cause: " + e, e);
    }
  }

  /**
   * 设置指定位置的参数的值，由子类实现
   */
  public abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

  /**
   * 获得指定名称的字段的值
   * 该方法由子类实现。
   */
  public abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;

  /**
   * 获得指定位置的字段的值
   * 该方法由子类实现。
   */
  public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;

}
