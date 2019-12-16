package cn.javadog.sd.mybatis.executor.resultset;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;
import cn.javadog.sd.mybatis.support.type.handler.ObjectTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.UnknownTypeHandler;

/**
 * @author 余勇
 * @date 2019-12-16 16:36
 *
 * java.sql.ResultSet 的 包装器，可以理解成 ResultSet 的工具类，提供给 DefaultResultSetHandler 使用
 */
public class ResultSetWrapper {

  /**
   * ResultSet 对象
   */
  private final ResultSet resultSet;

  /**
   * 类型处理器注册表
   */

  private final TypeHandlerRegistry typeHandlerRegistry;
  /**
   * 字段的名字的数组
   */
  private final List<String> columnNames = new ArrayList<>();

  /**
   * 字段的 Java Type 的数组
   */
  private final List<String> classNames = new ArrayList<>();

  /**
   * 字段的 JdbcType 的数组
   */
  private final List<JdbcType> jdbcTypes = new ArrayList<>();

  /**
   * TypeHandler 的映射
   *
   * KEY1：字段的名字
   * KEY2：Java 属性类型
   */
  private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<>();

  /**
   * 有 mapped 的字段的名字的映射。就是 数据库字段中，resultMap 也有对应关系的字段
   *
   * KEY：{@link #getMapKey(ResultMap, String)}
   * VALUE：字段的名字的数组
   */
  private final Map<String, List<String>> mappedColumnNamesMap = new HashMap<>();

  /**
   * 无 mapped 的字段的名字的映射。就是 数据库字段中，resultMap 没有对应关系的字段
   *
   * 和 {@link #mappedColumnNamesMap} 相反
   */
  private final Map<String, List<String>> unMappedColumnNamesMap = new HashMap<>();

  /**
   * 构造函数
   */
  public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {
    super();
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.resultSet = rs;
    // 遍历 ResultSetMetaData 的字段们，解析出 columnNames、jdbcTypes、classNames 属性
    final ResultSetMetaData metaData = rs.getMetaData();
    final int columnCount = metaData.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
      jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
      classNames.add(metaData.getColumnClassName(i));
    }
  }

  /**
   * 获取 resultSet
   */
  public ResultSet getResultSet() {
    return resultSet;
  }

  /**
   * 获取 columnNames
   */
  public List<String> getColumnNames() {
    return this.columnNames;
  }

  /**
   * 获取 classNames
   */
  public List<String> getClassNames() {
    return Collections.unmodifiableList(classNames);
  }

  /**jdbcTypes
   * 获取
   */
  public List<JdbcType> getJdbcTypes() {
    return jdbcTypes;
  }

  /**
   * 获取指定列的jdbcTypes
   */
  public JdbcType getJdbcType(String columnName) {
    for (int i = 0 ; i < columnNames.size(); i++) {
      if (columnNames.get(i).equalsIgnoreCase(columnName)) {
        return jdbcTypes.get(i);
      }
    }
    return null;
  }

  /**
   * 获得指定字段名的指定 JavaType 类型的 TypeHandler 对象
   */
  public TypeHandler<?> getTypeHandler(Class<?> propertyType, String columnName) {
    TypeHandler<?> handler = null;
    // 先从缓存的 typeHandlerMap 中，获得指定字段名的指定 JavaType 类型的 TypeHandler 对象
    Map<Class<?>, TypeHandler<?>> columnHandlers = typeHandlerMap.get(columnName);
    if (columnHandlers == null) {
      columnHandlers = new HashMap<>();
      typeHandlerMap.put(columnName, columnHandlers);
    } else {
      handler = columnHandlers.get(propertyType);
    }
    // 如果获取不到，则进行查找
    if (handler == null) {
      // 获得 JdbcType 类型
      JdbcType jdbcType = getJdbcType(columnName);
      // 获得 TypeHandler 对象
      handler = typeHandlerRegistry.getTypeHandler(propertyType, jdbcType);
      // 如果获取不到，则再次进行查找。和 UnknownTypeHandler#resolveTypeHandler 方法的逻辑一样。可以看看 issue #59 comment 10
      if (handler == null || handler instanceof UnknownTypeHandler) {
        // 使用 classNames 中的类型，进行继续查找 TypeHandler 对象
        final int index = columnNames.indexOf(columnName);
        final Class<?> javaType = resolveClass(classNames.get(index));
        if (javaType != null && jdbcType != null) {
          handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
        } else if (javaType != null) {
          handler = typeHandlerRegistry.getTypeHandler(javaType);
        } else if (jdbcType != null) {
          handler = typeHandlerRegistry.getTypeHandler(jdbcType);
        }
      }
      // 如果获取不到，则使用 ObjectTypeHandler 对象
      if (handler == null || handler instanceof UnknownTypeHandler) {
        handler = new ObjectTypeHandler();
      }
      // 缓存到 typeHandlerMap 中
      columnHandlers.put(propertyType, handler);
    }
    return handler;
  }

  /**
   * 获取指定类名的class对象
   */
  private Class<?> resolveClass(String className) {
    try {
      // #699 className could be null
      if (className != null) {
        return Resources.classForName(className);
      }
    } catch (ClassNotFoundException e) {
      // ignore
    }
    return null;
  }

  /**
   * 初始化有 mapped 和无 mapped 的字段的名字数组
   */
  private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    // mapped 字段名数组
    List<String> mappedColumnNames = new ArrayList<>();
    // 无 mapped 字段名数组
    List<String> unmappedColumnNames = new ArrayList<>();
    // 将 columnPrefix 转换成大写
    final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
    // 拼接到 resultMap.mappedColumns 属性上
    final Set<String> mappedColumns = prependPrefixes(resultMap.getMappedColumns(), upperColumnPrefix);
    // 遍历 columnNames 数组，根据是否在 mappedColumns 中，分别添加到 mappedColumnNames 和 unmappedColumnNames 中
    for (String columnName : columnNames) {
      // 列名转大写
      final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
      if (mappedColumns.contains(upperColumnName)) {
        // 包含，就添加到 mappedColumnNames
        mappedColumnNames.add(upperColumnName);
      } else {
        // 不包含，就添加到 unmappedColumnNames
        unmappedColumnNames.add(columnName);
      }
    }
    // 将 mappedColumnNames 和 unmappedColumnNames 结果，添加到 mappedColumnNamesMap 和 unMappedColumnNamesMap 中
    mappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), mappedColumnNames);
    unMappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), unmappedColumnNames);
  }

  /**
   * 获取指定 resultMap 能匹配上数据库列 的字段
   */
  public List<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    // 获得对应的 mapped 数组
    List<String> mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    if (mappedColumnNames == null) {
      // 初始化
      loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
      // 重新获得对应的 mapped 数组
      mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    }
    return mappedColumnNames;
  }

  /**
   * 获取数据库字段，在 resultMap 上取不到的字段
   */
  public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    // 获得对应的 unMapped 数组
    List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    if (unMappedColumnNames == null) {
      // 初始化
      loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
      // 重新获得对应的 unMapped 数组
      unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    }
    return unMappedColumnNames;
  }

  /**
   * 获取 resultMap 的 key。格式是 {resultMapId}:columnPrefix
   */
  private String getMapKey(ResultMap resultMap, String columnPrefix) {
    return resultMap.getId() + ":" + columnPrefix;
  }

  /**
   * 将前缀追加到列名上
   */
  private Set<String> prependPrefixes(Set<String> columnNames, String prefix) {
    // 直接返回 columnNames ，如果符合如下任一情况
    if (columnNames == null || columnNames.isEmpty() || prefix == null || prefix.length() == 0) {
      return columnNames;
    }
    // 拼接前缀 prefix ，然后返回
    final Set<String> prefixed = new HashSet<>();
    for (String columnName : columnNames) {
      prefixed.add(prefix + columnName);
    }
    return prefixed;
  }
  
}
