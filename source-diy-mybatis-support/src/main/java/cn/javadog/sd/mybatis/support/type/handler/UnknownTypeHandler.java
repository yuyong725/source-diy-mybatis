package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import cn.javadog.sd.mybatis.support.exceptions.TypeException;
import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 *
 *
 */
/**
 * @author 余勇
 * @date 2019-12-05 21:58
 * 未知的 TypeHandler 实现类, 就是没表明使用什么类型的解析器，其实我们写的xml一般都没有标明，最终由这个类去找的最匹配。
 * 通过获取对应的 TypeHandler ，进行处理。
 */
public class UnknownTypeHandler extends BaseTypeHandler<Object> {

  /**
   * ObjectTypeHandler 单例
   */
  private static final ObjectTypeHandler OBJECT_TYPE_HANDLER = new ObjectTypeHandler();

  /**
   * TypeHandler 注册表
   */
  private TypeHandlerRegistry typeHandlerRegistry;

  /**
   * 构造，需要typeHandlerRegistry，主逻辑都靠他
   */
  public UnknownTypeHandler(TypeHandlerRegistry typeHandlerRegistry) {
    this.typeHandlerRegistry = typeHandlerRegistry;
  }

  /**
   * 设置参数值
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
      throws SQLException {
    // 获得参数对应的处理器
    TypeHandler handler = resolveTypeHandler(parameter, jdbcType);
    // 使用 handler 设置参数
    handler.setParameter(ps, i, parameter, jdbcType);
  }

  /**
   * 获取指定名称的字段的值
   */
  @Override
  public Object getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    // 获得参数对应的处理器
    TypeHandler<?> handler = resolveTypeHandler(rs, columnName);
    // 使用 handler 获得值
    return handler.getResult(rs, columnName);
  }

  /**
   * 获取指定位置的字段的值
   */
  @Override
  public Object getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    // 获得参数对应的处理器
    TypeHandler<?> handler = resolveTypeHandler(rs.getMetaData(), columnIndex);
    // 如果找不到对应的处理器，使用 OBJECT_TYPE_HANDLER
    if (handler == null || handler instanceof UnknownTypeHandler) {
      handler = OBJECT_TYPE_HANDLER;
    }
    // 使用 handler 获得值
    return handler.getResult(rs, columnIndex);
  }

  /**
   * 挑选最合适的处理器，用于设置参数
   */
  private TypeHandler<? extends Object> resolveTypeHandler(Object parameter, JdbcType jdbcType) {
    TypeHandler<? extends Object> handler;
    if (parameter == null) {
      // 参数为空，返回 OBJECT_TYPE_HANDLER
      handler = OBJECT_TYPE_HANDLER;
    } else {
      // 参数非空，使用参数类型获得对应的 TypeHandler
      handler = typeHandlerRegistry.getTypeHandler(parameter.getClass(), jdbcType);
      // 获取不到，则使用 OBJECT_TYPE_HANDLER
      if (handler == null || handler instanceof UnknownTypeHandler) {
        handler = OBJECT_TYPE_HANDLER;
      }
    }
    return handler;
  }

  /**
   * 挑选最合适的处理器，用于解析结果，根据字段名
   */
  private TypeHandler<?> resolveTypeHandler(ResultSet rs, String column) {
    try {
      // 获取所有的字段，按顺序添加到columnIndexLookup
      Map<String,Integer> columnIndexLookup;
      columnIndexLookup = new HashMap<>();
      ResultSetMetaData rsmd = rs.getMetaData();
      int count = rsmd.getColumnCount();
      for (int i=1; i <= count; i++) {
        String name = rsmd.getColumnName(i);
        columnIndexLookup.put(name,i);
      }
      // 获取指定字段的角标
      Integer columnIndex = columnIndexLookup.get(column);
      TypeHandler<?> handler = null;
      // 首先，通过 columnIndex 获得 TypeHandler
      if (columnIndex != null) {
        handler = resolveTypeHandler(rsmd, columnIndex);
      }
      // 获得不到，使用 OBJECT_TYPE_HANDLER
      if (handler == null || handler instanceof UnknownTypeHandler) {
        handler = OBJECT_TYPE_HANDLER;
      }
      return handler;
    } catch (SQLException e) {
      throw new TypeException("Error determining JDBC type for column " + column + ".  Cause: " + e, e);
    }
  }

  /**
   * 挑选最合适的处理器，用于解析结果，根据角标
   */
  private TypeHandler<?> resolveTypeHandler(ResultSetMetaData rsmd, Integer columnIndex) {
    TypeHandler<?> handler = null;
    // 获得 JDBC Type 类型
    JdbcType jdbcType = safeGetJdbcTypeForColumn(rsmd, columnIndex);
    // 获得 Java Type 类型
    Class<?> javaType = safeGetClassForColumn(rsmd, columnIndex);
    //获得对应的 TypeHandler 对象
    if (javaType != null && jdbcType != null) {
      handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
    } else if (javaType != null) {
      handler = typeHandlerRegistry.getTypeHandler(javaType);
    } else if (jdbcType != null) {
      handler = typeHandlerRegistry.getTypeHandler(jdbcType);
    }
    return handler;
  }

  /**
   *
   */
  private JdbcType safeGetJdbcTypeForColumn(ResultSetMetaData rsmd, Integer columnIndex) {
    try {
      // 从 ResultSetMetaData 中，获得字段类型
      // 获得 JDBC Type
      return JdbcType.forCode(rsmd.getColumnType(columnIndex));
    } catch (Exception e) {
      return null;
    }
  }

  private Class<?> safeGetClassForColumn(ResultSetMetaData rsmd, Integer columnIndex) {
    try {
      // 从 ResultSetMetaData 中，获得字段类型
      // 获得 Java Type
      return Resources.classForName(rsmd.getColumnClassName(columnIndex));
    } catch (Exception e) {
      return null;
    }
  }
}
