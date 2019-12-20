package cn.javadog.sd.mybatis.executor.resultset;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.javadog.sd.mybatis.annotations.AutomapConstructor;
import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.cursor.defaults.DefaultCursor;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.executor.loader.ResultLoader;
import cn.javadog.sd.mybatis.executor.loader.ResultLoaderMap;
import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.executor.result.DefaultResultContext;
import cn.javadog.sd.mybatis.executor.result.DefaultResultHandler;
import cn.javadog.sd.mybatis.executor.result.ResultContext;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.Discriminator;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.mapping.ResultMapping;
import cn.javadog.sd.mybatis.session.AutoMappingBehavior;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.cache.CacheKey;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;
import cn.javadog.sd.mybatis.support.exceptions.ResultMapException;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ReflectorFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaClass;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.ParamMap;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author 余勇
 * @date 2019-12-15 17:41
 * 实现 ResultSetHandler 接口，默认的 ResultSetHandler 实现类
 */
public class DefaultResultSetHandler implements ResultSetHandler {

  /**
   * 标记着待定的对象
   */
  private static final Object DEFERED = new Object();

  /**
   * 执行器
   */
  private final Executor executor;

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * 对应的 MappedStatement
   */
  private final MappedStatement mappedStatement;

  /**
   * 对应的 RowBounds
   */
  private final RowBounds rowBounds;

  /**
   * 对应的 ParameterHandler
   */
  private final ParameterHandler parameterHandler;

  /**
   * 用户指定的用于处理结果的处理器。
   *
   * 一般情况下，不设置
   */
  private final ResultHandler<?> resultHandler;

  /**
   * SQL对象 BoundSql
   */
  private final BoundSql boundSql;

  /**
   * 类型转换器注册表 TypeHandlerRegistry
   */
  private final TypeHandlerRegistry typeHandlerRegistry;

  /**
   * 对象工厂 ObjectFactory
   */
  private final ObjectFactory objectFactory;

  /**
   * 反射工厂 ReflectorFactory
   */
  private final ReflectorFactory reflectorFactory;

  /**
   * 内嵌的 resultmap
   * key -> 每行结果的唯一标示，存在 CacheKey 中
   * value -> 嵌套的结果对象
   */
  private final Map<CacheKey, Object> nestedResultObjects = new HashMap<>();

  /**
   * TODO 祖先对象map，啥玩意？
   */
  private final Map<String, Object> ancestorObjects = new HashMap<>();

  /**
   * 上一行的结果，返回的结果集可能是个列表，逐行去取时，使用此字段临时记录上一行的结果
   */
  private Object previousRowValue;

  /**
   * PendingRelation是DefaultResultSetHandler的内部静态类，记录了当前结果对象对应的MetaObject对象以及parentMapping对象
   * 该对象就为CacheKey对象跟全部的PendingRelation对象的映射
   */
  private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<>();

  /**
   * 自动映射的缓存
   *
   * KEY：{@link ResultMap#getId()} + ":" +  columnPrefix
   *
   * @see #createRowKeyForUnmappedProperties(ResultMap, ResultSetWrapper, CacheKey, String)
   */
  private final Map<String, List<UnMappedColumnAutoMapping>> autoMappingsCache = new HashMap<>();

  /**
   * 临时标记是否使用构造方法创建该结果对象。使用此字段减少内存占用
   */
  private boolean useConstructorMappings;

  /**
   * 内部类，待定关系
   */
  private static class PendingRelation {

    /**
     * 元对象
     */
    public MetaObject metaObject;

    /**
     * 结果映射
     */
    public ResultMapping propertyMapping;
  }

  /**
   * 未在 resultMap 中显示声明 数据库字段 与 POJO字段 映射关系的列名信息。
   * 即使未显示声明，但如果开启了字段自动映射，数据库查询结果依然会写到POJO相应字段
   */
  private static class UnMappedColumnAutoMapping {

    /**
     * 字段名
     */
    private final String column;

    /**
     * 属性名
     */
    private final String property;

    /**
     * TypeHandler 处理器
     */
    private final TypeHandler<?> typeHandler;

    /**
     * 是否为基本属性
     */
    private final boolean primitive;

    /**
     * 构造函数
     */
    public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler, boolean primitive) {
      this.column = column;
      this.property = property;
      this.typeHandler = typeHandler;
      this.primitive = primitive;
    }
  }

  /**
   * 构造函数
   */
  public DefaultResultSetHandler(Executor executor, MappedStatement mappedStatement, ParameterHandler parameterHandler, ResultHandler<?> resultHandler, BoundSql boundSql,
                                 RowBounds rowBounds) {
    this.executor = executor;
    this.configuration = mappedStatement.getConfiguration();
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;
    this.parameterHandler = parameterHandler;
    this.boundSql = boundSql;
    // 从 configuration 中拿
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();
    this.reflectorFactory = configuration.getReflectorFactory();
    this.resultHandler = resultHandler;
  }


  /**
   * 处理 java.sql.ResultSet 结果集，转换成映射的对应结果.
   */
  @Override
  public List<Object> handleResultSets(Statement stmt) throws SQLException {
    // 异常上下文记录一笔
    ErrorContext.instance().activity("handling results").object(mappedStatement.getId());
    // 多 ResultSet 的结果集合，每个 ResultSet 对应一个 Object 对象。而实际上，每个 Object 是 List<Object> 对象。
    // 获得首个 ResultSet 对象，并封装成 ResultSetWrapper 对象
    ResultSetWrapper rsw = getFirstResultSet(stmt);
    // 获得 ResultMap
    ResultMap resultMap = mappedStatement.getResultMap();
    // 校验
    validateResultMap(rsw, resultMap);
    // 处理 ResultSet ，将结果添加到 multipleResults 中
    List<Object> result = handleResultSet(rsw, resultMap);
    // 清空所有的嵌套结果集对象
    cleanUpAfterHandlingResultSet();
    // 如果是 multipleResults 单元素，则取首元素返回
    return result;
  }

  /**
   * 处理 java.sql.ResultSet 成 Cursor 对象
   */
  @Override
  public <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException {
    ErrorContext.instance().activity("handling cursor results").object(mappedStatement.getId());
   // 获得首个 ResultSet 对象，并封装成 ResultSetWrapper 对象
    ResultSetWrapper rsw = getFirstResultSet(stmt);
    // 游标方式的查询，只允许一个 ResultSet 对象。因此，resultMaps 数组的数量，元素只能有一个
    ResultMap resultMap = mappedStatement.getResultMap();
    validateResultMap(rsw, resultMap);
    // 创建 DefaultCursor 对象
    return new DefaultCursor<>(this, resultMap, rsw, rowBounds);
  }

  /**
   * 获得首个 ResultSet 对象，并封装成 ResultSetWrapper 对象
   */
  private ResultSetWrapper getFirstResultSet(Statement stmt) throws SQLException {
    ResultSet rs = stmt.getResultSet();
    // 可以忽略
    while (rs == null) {
      // 向下遍历，直到拿到第一个 resultset。因为有的驱动不会将 resultset 放在最前面，例如(HSQLDB 2.1)
      if (stmt.getMoreResults()) {
        rs = stmt.getResultSet();
      } else {
        if (stmt.getUpdateCount() == -1) {
          // 遍历到最后了，自然没有了
          break;
        }
      }
    }
    // 将 ResultSet 对象，封装成 ResultSetWrapper 对象
    return rs != null ? new ResultSetWrapper(rs, configuration) : null;
  }

  /**
   * 关闭 ResultSet
   */
  private void closeResultSet(ResultSet rs) {
    try {
      if (rs != null) {
        rs.close();
      }
    } catch (SQLException e) {
      // ignore
    }
  }

  /**
   * 处理完结果集后清空 nestedResultObjects
   */
  private void cleanUpAfterHandlingResultSet() {
    nestedResultObjects.clear();
  }

  /**
   * 校验至少有一个 ResultMap 对象
   */
  private void validateResultMap(ResultSetWrapper rsw, ResultMap resultMap) {
    if (rsw != null && resultMap == null) {
      throw new ExecutorException("A query was run and no Result Maps were found for the Mapped Statement '" + mappedStatement.getId()
          + "'.  It's likely that neither a Result Type nor a Result Map was specified.");
    }
    if (resultMap == null) {
      throw new ExecutorException("Cursor results cannot be mapped to multiple resultMaps");
    }
  }

  /**
   * 处理 ResultSet ，将结果添加到 multipleResults
   */
  private List<Object> handleResultSet(ResultSetWrapper rsw, ResultMap resultMap) throws SQLException {
    try {
      // 如果没有自定义的 resultHandler ，则创建默认的 DefaultResultHandler 对象
      if (resultHandler == null) {
        // 创建 DefaultResultHandler 对象
        DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
        // 处理 ResultSet 返回的每一行 Row
        handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
        // 添加 defaultResultHandler 的处理的结果，到 multipleResults 中
        return defaultResultHandler.getResultList();
      } else {
        // 处理 ResultSet 返回的每一行 Row，note 自定义的 resultHandler 的结果为什么不返还
        handleRowValues(rsw, resultMap, resultHandler, rowBounds, null);
        return new ArrayList<>();
      }
    } finally {
      // 关闭 ResultSet 对象
      closeResultSet(rsw.getResultSet());
    }
  }

  /**
   * 处理 ResultSet 返回的每一行 Row
   */
  public void handleRowValues(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
    // 处理嵌套映射的情况
    if (resultMap.hasNestedResultMaps()) {
      // 校验是否可以使用 RowBounds
      ensureNoRowBounds();
      // 校验是否可以使用自定义的 resultHandler
      checkResultHandler();
      // 处理嵌套映射的结果
      handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
    } else {
      // 处理简单映射的结果
      handleRowValuesForSimpleResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
    }
  }

  /**
   * 确保可以安全使用 RowBounds 分页条件
   * safeRowBoundsEnabled: 允许在嵌套语句中使用分页（RowBounds）。如果允许使用则设置为 false。
   */
  private void ensureNoRowBounds() {
    // 如果不允许使用嵌套分页，但是 使用了分页条件，且分页条件是合理的(也就是并非无效的分页条件)，那么就GG
    if (configuration.isSafeRowBoundsEnabled() && rowBounds != null && (rowBounds.getLimit() < RowBounds.NO_ROW_LIMIT || rowBounds.getOffset() > RowBounds.NO_ROW_OFFSET)) {
      throw new ExecutorException("Mapped Statements with nested result mappings cannot be safely constrained by RowBounds. "
          + "Use safeRowBoundsEnabled=false setting to bypass this check.");
    }
  }

  /**
   * 确保可以可以使用自定义的 ResultHandler
   * safeResultHandlerEnabled：允许在嵌套语句中使用结果处理器（ResultHandler）。如果允许使用则设置为 false
   * resultOrdered：这个设置仅针对嵌套结果 select 语句适用：如果为 true，就是假设包含了嵌套结果集或是分组，这样的话当返回一个主结果行的时候，
   *  就不会发生有对前面结果集的引用的情况。这就使得在获取嵌套的结果集的时候不至于导致内存不够用。默认值：false。
   */
  protected void checkResultHandler() {
    // 如果不允许在嵌套语句中使用结果处理器，但是使用了resultHandler，直接GG
    if (resultHandler != null && configuration.isSafeResultHandlerEnabled() && !mappedStatement.isResultOrdered()) {
      throw new ExecutorException("Mapped Statements with nested result mappings cannot be safely used with a custom ResultHandler. "
          + "Use safeResultHandlerEnabled=false setting to bypass this check "
          + "or ensure your statement returns ordered data and set resultOrdered=true on it.");
    }
  }

  /**
   * 处理简单映射的结果
   */
  private void handleRowValuesForSimpleResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping)
      throws SQLException {
    // 创建 DefaultResultContext 对象
    DefaultResultContext<Object> resultContext = new DefaultResultContext<>();
    // 获得 ResultSet 对象，并跳到 rowBounds 指定的开始位置
    ResultSet resultSet = rsw.getResultSet();
    // 跳过分页
    skipRows(resultSet, rowBounds);
    // 循环，条件： 是否继续处理 ResultSet(resultContext没有关闭) + ResultSet 是否已经关闭 + ResultSet 是否还有下一条
    while (shouldProcessMoreRows(resultContext, rowBounds) && !resultSet.isClosed() && resultSet.next()) {
      // 根据该行记录以及 ResultMap.discriminator ，决定映射使用的 ResultMap 对象。这个 resultMap 对应另一个对象，也就是所谓的关联对象
      ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(resultSet, resultMap, null);
      // 根据最终确定的 ResultMap 对 ResultSet 中的该行记录进行映射，得到映射后的结果对象
      Object rowValue = getRowValue(rsw, discriminatedResultMap, null);
      // 将映射创建的结果对象添加到 ResultHandler.resultList 中保存
      storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
    }
  }

  /**
   * 嵌套映射
   * 处理嵌套映射的结果
   */
  private void handleRowValuesForNestedResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
    // 初始化一个 resultContext
    final DefaultResultContext<Object> resultContext = new DefaultResultContext<>();
    // 获取 ResultSet
    ResultSet resultSet = rsw.getResultSet();
    // 跳过分页
    skipRows(resultSet, rowBounds);
    // 记录下上一行的结果
    Object rowValue = previousRowValue;
    // 遍历。直到达到分页的 limit
    while (shouldProcessMoreRows(resultContext, rowBounds) && !resultSet.isClosed() && resultSet.next()) {
      // 解析 <discriminator /> 标签对应字段的 ResultMap，之所以特殊处理，因为其会产生内嵌查询，懒加载等
      final ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(resultSet, resultMap, null);
      // 获取一行结果的唯一键
      final CacheKey rowKey = createRowKey(discriminatedResultMap, rsw, null);
      // TODO 这是啥玩意
      Object partialObject = nestedResultObjects.get(rowKey);
      // issue #577 && #542
      if (mappedStatement.isResultOrdered()) {
        if (partialObject == null && rowValue != null) {
          // 清空 nestedResultObjects
          nestedResultObjects.clear();
          // 存储查到的结果
          storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
        }
        // 获取下一行结果，一开始 rowValue 是上一行的结果，然后进行了一系列的存储操作
        rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, null, partialObject);
      } else {
        rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, null, partialObject);
        if (partialObject == null) {
          storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
        }
      }
    }
    if (rowValue != null && mappedStatement.isResultOrdered() && shouldProcessMoreRows(resultContext, rowBounds)) {
      storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
      previousRowValue = null;
    } else if (rowValue != null) {
      previousRowValue = rowValue;
    }
  }


  /**
   * 将映射创建的结果对象添加到 ResultHandler.resultList 中保存
   */
  private void storeObject(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext, Object rowValue, ResultMapping parentMapping, ResultSet rs) throws SQLException {
    // 将结果存起来
    callResultHandler(resultHandler, resultContext, rowValue);
  }

  /**
   * 调用 ResultHandler ，进行结果的处理
   */
  @SuppressWarnings("unchecked" /* because ResultHandler<?> is always ResultHandler<Object>*/)
  private void callResultHandler(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext, Object rowValue) {
    // 设置结果对象到 resultContext 中
    resultContext.nextResultObject(rowValue);
    // 使用 ResultHandler 处理结果。
    // 如果使用 DefaultResultHandler 实现类的情况，会将映射创建的结果对象添加到 ResultHandler.resultList 中保存
    ((ResultHandler<Object>) resultHandler).handleResult(resultContext);
  }

  /**
   * 是否可以解析更多的结果
   */
  private boolean shouldProcessMoreRows(ResultContext<?> context, RowBounds rowBounds) {
    // 结果上下文未关闭 && 当前取的结果行数<分页条件要取的结果行数
    return !context.isStopped() && context.getResultCount() < rowBounds.getLimit();
  }

  /**
   * 跳到 rowBounds 指定的开始位置
   */
  private void skipRows(ResultSet rs, RowBounds rowBounds) throws SQLException {
    if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
      // 直接跳转到指定开始的位置
      if (rowBounds.getOffset() != RowBounds.NO_ROW_OFFSET) {
        rs.absolute(rowBounds.getOffset());
      }
    } else {
      // TYPE_FORWARD_ONLY 的 ResultSet 不能跳到指定位置，只能循环，不断跳到开始的位置
      for (int i = 0; i < rowBounds.getOffset(); i++) {
        if (!rs.next()) {
          break;
        }
      }
    }
  }

  /**
   * 根据最终确定的 ResultMap 对 ResultSet 中的该行记录进行映射，得到映射后的结果对象
   */
  private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix) throws SQLException {
    // 创建 ResultLoaderMap 对象
    final ResultLoaderMap lazyLoader = new ResultLoaderMap();
    // 创建映射后的结果对象
    Object rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
    // 如果 hasTypeHandlerForResultObject(rsw, resultMap.getType()) 返回 true ，意味着 rowValue 是基本类型，无需执行下列逻辑。
    if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
      // 创建 MetaObject 对象，用于访问 rowValue 对象
      final MetaObject metaObject = configuration.newMetaObject(rowValue);
      // foundValues 代表，是否成功映射任一属性。若成功，则为 true ，若失败，则为 false
      boolean foundValues = this.useConstructorMappings;
      /// 判断是否开启自动映射功能
      if (shouldApplyAutomaticMappings(resultMap, false)) {
        // 自动映射未明确的列
        foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
      }
      // 映射 ResultMap 中明确映射的列
      foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
      // ↑↑↑ 至此，当前 ResultSet 的该行记录的数据，已经完全映射到结果对象 rowValue 的对应属性种
      foundValues = lazyLoader.size() > 0 || foundValues;
      // 如果没有成功映射任意属性，则置空 rowValue 对象。
      // 当然，如果开启 `configuration.returnInstanceForEmptyRow` 属性，则不置空。默认情况下，该值为 false
      rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
    }
    return rowValue;
  }

  /**
   * 判断是否使用自动映射的功能
   */
  private boolean shouldApplyAutomaticMappings(ResultMap resultMap, boolean isNested) {
    // 判断是否开启自动映射功能
    if (resultMap.getAutoMapping() != null) {
      return resultMap.getAutoMapping();
    } else {
      // 内嵌查询或嵌套映射时
      if (isNested) {
        return AutoMappingBehavior.FULL == configuration.getAutoMappingBehavior(); // 需要 FULL
        // 普通映射
      } else {
        return AutoMappingBehavior.NONE != configuration.getAutoMappingBehavior();
      }
    }
  }

  /**
   * 映射 ResultMap 中明确映射的列
   */
  private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    // 获得 mapped 的字段的名字的数组
    final List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
    boolean foundValues = false;
    // 遍历 ResultMapping 数组
    final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
    for (ResultMapping propertyMapping : propertyMappings) {
      // 获得字段名
      String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
      if (propertyMapping.getNestedResultMapId() != null) {
        // the user added a column attribute to a nested result map, ignore it
        column = null;
      }
      if (propertyMapping.isCompositeResult() // 组合
          // 属于mappedColumnNames)
              || (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH)))){
        // 获得指定字段的值
        Object value = getPropertyMappingValue(rsw.getResultSet(), metaObject, propertyMapping, lazyLoader, columnPrefix);
        // issue #541 make property optional
        final String property = propertyMapping.getProperty();
        if (property == null) {
          continue;
          // 存储过程相关，忽略
        } else if (value == DEFERED) {
          foundValues = true;
          continue;
        }
        // 标记获取到任一属性
        if (value != null) {
          foundValues = true;
        }
        // 设置到 parameterObject 中，通过 metaObject
        if (value != null || (configuration.isCallSettersOnNulls() && !metaObject.getSetterType(property).isPrimitive())) {
          // gcode issue #377, call setter on nulls (value is not 'found')
          metaObject.setValue(property, value);
        }
      }
    }
    return foundValues;
  }

  /**
   * 获得指定字段的
   */
  private Object getPropertyMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    // 内嵌查询，获得嵌套查询的值
    if (propertyMapping.getNestedQueryId() != null) {
      return getNestedQueryMappingValue(rs, metaResultObject, propertyMapping, lazyLoader, columnPrefix);
    }
    // 普通，直接获得指定字段的值
    else {
      final TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
      final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
      return typeHandler.getResult(rs, column);
    }
  }

  /**
   * 针对未匹配上的列，创建相应的自动匹配关系
   */
  private List<UnMappedColumnAutoMapping> createAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
    // 生成 autoMappingsCache 的 KEY
    final String mapKey = resultMap.getId() + ":" + columnPrefix;
    // 从缓存 autoMappingsCache 中，获得 UnMappedColumnAutoMapping 数组
    List<UnMappedColumnAutoMapping> autoMapping = autoMappingsCache.get(mapKey);
    // 如果获取不到，则进行初始化
    if (autoMapping == null) {
      autoMapping = new ArrayList<>();
      // 获得未 mapped 的字段的名字的数组
      final List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
      // 遍历 unmappedColumnNames 数组
      for (String columnName : unmappedColumnNames) {
        // 获得属性名
        String propertyName = columnName;
        if (columnPrefix != null && !columnPrefix.isEmpty()) {
          // When columnPrefix is specified,
          // ignore columns without the prefix.
          if (columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
            propertyName = columnName.substring(columnPrefix.length());
          } else {
            continue;
          }
        }
        // 从结果对象的 metaObject 中，获得对应的属性名
        final String property = metaObject.findProperty(propertyName, configuration.isMapUnderscoreToCamelCase());
        // 获得到属性名，并且可以进行设置
        if (property != null && metaObject.hasSetter(property)) {
          // 排除已映射的属性
          if (resultMap.getMappedProperties().contains(property)) {
            continue;
          }
          // 获得属性的类型
          final Class<?> propertyType = metaObject.getSetterType(property);
          // 判断是否有对应的 TypeHandler 对象。如果有，则创建 UnMappedColumnAutoMapping 对象，并添加到 autoMapping 中
          if (typeHandlerRegistry.hasTypeHandler(propertyType, rsw.getJdbcType(columnName))) {
            final TypeHandler<?> typeHandler = rsw.getTypeHandler(propertyType, columnName);
            autoMapping.add(new UnMappedColumnAutoMapping(columnName, property, typeHandler, propertyType.isPrimitive()));
            // 如果没有，则执行 AutoMappingUnknownColumnBehavior 对应的逻辑
          } else {
            configuration.getAutoMappingUnknownColumnBehavior()
                    .doAction(mappedStatement, columnName, property, propertyType);
          }
          // 如果没有属性，或者无法设置，则则执行 AutoMappingUnknownColumnBehavior 对应的逻辑
        } else {
          configuration.getAutoMappingUnknownColumnBehavior()
                  .doAction(mappedStatement, columnName, (property != null) ? property : propertyName, null);
        }
      }
      // 添加到缓存中
      autoMappingsCache.put(mapKey, autoMapping);
    }
    return autoMapping;
  }

  /**
   * 创建映射后的结果对象
   */
  private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
    // 获得 UnMappedColumnAutoMapping 数组
    List<UnMappedColumnAutoMapping> autoMapping = createAutomaticMappings(rsw, resultMap, metaObject, columnPrefix);
    boolean foundValues = false;
    if (!autoMapping.isEmpty()) {
      // 遍历 UnMappedColumnAutoMapping 数组
      for (UnMappedColumnAutoMapping mapping : autoMapping) {
        // 获得指定字段的值
        final Object value = mapping.typeHandler.getResult(rsw.getResultSet(), mapping.column);
        // 若非空，标记 foundValues 有值
        if (value != null) {
          foundValues = true;
        }
        // 设置到 parameterObject 中，通过 metaObject
        if (value != null || (configuration.isCallSettersOnNulls() && !mapping.primitive)) {
          // gcode issue #377, call setter on nulls (value is not 'found')
          metaObject.setValue(mapping.property, value);
        }
      }
    }
    return foundValues;
  }

  /**
   * 创建映射后的结果对象
   */
  private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
    // useConstructorMappings ，表示是否使用构造方法创建该结果对象。此处将其重置
    this.useConstructorMappings = false;
    // 记录使用的构造方法的参数类型的数组
    final List<Class<?>> constructorArgTypes = new ArrayList<>();
    // 记录使用的构造方法的参数值的数组
    final List<Object> constructorArgs = new ArrayList<>();
    // 创建映射后的结果对象
    Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
    if (resultObject != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
      // 如果有内嵌的查询，并且开启延迟加载，则创建结果对象的代理对象
      final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
      for (ResultMapping propertyMapping : propertyMappings) {
        // issue gcode #109 && issue #149
        if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
          resultObject = configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
          break;
        }
      }
    }
    // 判断是否使用构造方法创建该结果对象
    this.useConstructorMappings = resultObject != null && !constructorArgTypes.isEmpty(); // set current mapping result
    return resultObject;
  }

  /**
   * 创建映射后的结果对象
   */
  private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix)
      throws SQLException {
    // 获取 resultMap 的类型
    final Class<?> resultType = resultMap.getType();
    // 获取类的元信息
    final MetaClass metaType = MetaClass.forClass(resultType, reflectorFactory);
    // 获取构造参数
    final List<ResultMapping> constructorMappings = resultMap.getConstructorResultMappings();
    // 下面，分成四种创建结果对象的情况
    // 情况一，如果有对应的 TypeHandler 对象，则意味着是基本类型，直接创建对结果应对象
    if (hasTypeHandlerForResultObject(rsw, resultType)) {
      return createPrimitiveResultObject(rsw, resultMap, columnPrefix);
    }
    // 情况二，如果 ResultMap 中，如果定义了 `<constructor />` 节点，则通过反射调用该构造方法，创建对应结果对象
    else if (!constructorMappings.isEmpty()) {
      return createParameterizedResultObject(rsw, resultType, constructorMappings, constructorArgTypes, constructorArgs, columnPrefix);
    }
    // 情况三，如果有默认的无参的构造方法，则使用该构造方法，创建对应结果对象
    else if (resultType.isInterface() || metaType.hasDefaultConstructor()) {
      return objectFactory.create(resultType);
    }
    // 情况四，通过自动映射的方式查找合适的构造方法，后使用该构造方法，创建对应结果对象
    else if (shouldApplyAutomaticMappings(resultMap, false)) {
      return createByConstructorSignature(rsw, resultType, constructorArgTypes, constructorArgs, columnPrefix);
    }
    // 不支持，抛出 ExecutorException 异常
    throw new ExecutorException("Do not know how to create an instance of " + resultType);
  }

  /**
   * 通过反射调用该构造方法，创建对应结果对象
   */
  Object createParameterizedResultObject(ResultSetWrapper rsw, Class<?> resultType, List<ResultMapping> constructorMappings,
                                         List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix) {
    // 获得到任一的属性值。即，只要一个结果对象，有一个属性非空，就会设置为 true
    boolean foundValues = false;
    for (ResultMapping constructorMapping : constructorMappings) {
      // 获得参数类型
      final Class<?> parameterType = constructorMapping.getJavaType();
      // 获得数据库的字段名
      final String column = constructorMapping.getColumn();
      // 获得属性值
      final Object value;
      try {
        // 如果是内嵌的查询，则获得内嵌的值
        if (constructorMapping.getNestedQueryId() != null) {
          value = getNestedQueryConstructorValue(rsw.getResultSet(), constructorMapping, columnPrefix);
          // 如果是内嵌的 resultMap ，则递归 getRowValue 方法，获得对应的属性值
        } else if (constructorMapping.getNestedResultMapId() != null) {
          final ResultMap resultMap = configuration.getResultMap(constructorMapping.getNestedResultMapId());
          value = getRowValue(rsw, resultMap, constructorMapping.getColumnPrefix());
          // 最常用的情况，直接使用 TypeHandler 获取当前 ResultSet 的当前行的指定字段的值
        } else {
          final TypeHandler<?> typeHandler = constructorMapping.getTypeHandler();
          value = typeHandler.getResult(rsw.getResultSet(), prependPrefix(column, columnPrefix));
        }
      } catch (ResultMapException | SQLException e) {
        throw new ExecutorException("Could not process result for mapping: " + constructorMapping, e);
      }
      // 添加到 constructorArgTypes 和 constructorArgs 中
      constructorArgTypes.add(parameterType);
      constructorArgs.add(value);
      // 判断是否获得到属性值
      foundValues = value != null || foundValues;
    }
    // 查找 constructorArgTypes 对应的构造方法
    // 查找到后，传入 constructorArgs 作为参数，创建结果对象
    return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
  }

  /**
   * 通过自动映射的方式查找合适的构造方法，后使用该构造方法，创建对应结果对象
   */
  private Object createByConstructorSignature(ResultSetWrapper rsw, Class<?> resultType, List<Class<?>> constructorArgTypes, List<Object> constructorArgs,
                                              String columnPrefix) throws SQLException {
    // 获得所有构造方法
    final Constructor<?>[] constructors = resultType.getDeclaredConstructors();
    // 获得默认构造方法
    final Constructor<?> defaultConstructor = findDefaultConstructor(constructors);
    // 如果有默认构造方法，使用该构造方法，创建结果对象
    if (defaultConstructor != null) {
      return createUsingConstructor(rsw, resultType, constructorArgTypes, constructorArgs, columnPrefix, defaultConstructor);
    } else {
      // 遍历所有构造方法，查找符合的构造方法，创建结果对象
      for (Constructor<?> constructor : constructors) {
        if (allowedConstructorUsingTypeHandlers(constructor, rsw.getJdbcTypes())) {
          return createUsingConstructor(rsw, resultType, constructorArgTypes, constructorArgs, columnPrefix, constructor);
        }
      }
    }
    throw new ExecutorException("No constructor found in " + resultType.getName() + " matching " + rsw.getClassNames());
  }

  /**
   * 使用该构造方法，创建结果对象
   */
  private Object createUsingConstructor(ResultSetWrapper rsw, Class<?> resultType, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix, Constructor<?> constructor) throws SQLException {
    boolean foundValues = false;
    for (int i = 0; i < constructor.getParameterTypes().length; i++) {
      // 获得参数类型
      Class<?> parameterType = constructor.getParameterTypes()[i];
      // 获得数据库的字段名
      String columnName = rsw.getColumnNames().get(i);
      // 获得 TypeHandler 对象
      TypeHandler<?> typeHandler = rsw.getTypeHandler(parameterType, columnName);
      // 获取当前 ResultSet 的当前行的指定字段的值
      Object value = typeHandler.getResult(rsw.getResultSet(), prependPrefix(columnName, columnPrefix));
      // 添加到 constructorArgTypes 和 constructorArgs 中
      constructorArgTypes.add(parameterType);
      constructorArgs.add(value);
      // 判断是否获得到属性值
      foundValues = value != null || foundValues;
    }
    // 查找 constructorArgTypes 对应的构造方法
    // 查找到后，传入 constructorArgs 作为参数，创建结果对象
    return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
  }

  /**
   * 获取默认构造
   */
  private Constructor<?> findDefaultConstructor(final Constructor<?>[] constructors) {
    // 构造方法只有一个，直接返回
    if (constructors.length == 1) {
      return constructors[0];
    }
    // 获得使用 @AutomapConstructor 注解的构造方法
    for (final Constructor<?> constructor : constructors) {
      if (constructor.isAnnotationPresent(AutomapConstructor.class)) {
        return constructor;
      }
    }
    return null;
  }

  /**
   * 查找符合的构造方法，后创建结果对象
   */
  private boolean allowedConstructorUsingTypeHandlers(final Constructor<?> constructor, final List<JdbcType> jdbcTypes) {
    final Class<?>[] parameterTypes = constructor.getParameterTypes();
    // 结果集的返回字段的数量，要和构造方法的参数数量，一致
    if (parameterTypes.length != jdbcTypes.size()) {
      return false;
    }
    // 每个构造方法的参数，和对应的返回字段，都要有对应的 TypeHandler 对象
    for (int i = 0; i < parameterTypes.length; i++) {
      if (!typeHandlerRegistry.hasTypeHandler(parameterTypes[i], jdbcTypes.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * 基本类型，直接创建对结果应对象
   */
  private Object createPrimitiveResultObject(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix) throws SQLException {
    final Class<?> resultType = resultMap.getType();
    // 获得字段名
    final String columnName;
    if (!resultMap.getResultMappings().isEmpty()) {
      final List<ResultMapping> resultMappingList = resultMap.getResultMappings();
      final ResultMapping mapping = resultMappingList.get(0);
      columnName = prependPrefix(mapping.getColumn(), columnPrefix);
    } else {
      columnName = rsw.getColumnNames().get(0);
    }
    // 获得 TypeHandler 对象
    final TypeHandler<?> typeHandler = rsw.getTypeHandler(resultType, columnName);
    // 获得 ResultSet 的指定字段的值
    return typeHandler.getResult(rsw.getResultSet(), columnName);
  }

  /**
   * 获得嵌套查询的
   */
  private Object getNestedQueryConstructorValue(ResultSet rs, ResultMapping constructorMapping, String columnPrefix) throws SQLException {
    // 获得内嵌查询的编号
    final String nestedQueryId = constructorMapping.getNestedQueryId();
    // 获得内嵌查询的 MappedStatement 对象
    final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
    // 获得内嵌查询的参数类型
    final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
    // 获得内嵌查询的参数对象
    final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rs, constructorMapping, nestedQueryParameterType, columnPrefix);
    Object value = null;
    // 执行查询
    if (nestedQueryParameterObject != null) {
      // 获得 BoundSql 对象
      final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
      // 获得 CacheKey 对象
      final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT, nestedBoundSql);
      final Class<?> targetType = constructorMapping.getJavaType();
      // 创建 ResultLoader 对象
      final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, key, nestedBoundSql);
      // 加载结果
      value = resultLoader.loadResult();
    }
    return value;
  }

  /**
   * 获得嵌套查询的值
   */
  private Object getNestedQueryMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    // 获得内嵌查询的编号
    final String nestedQueryId = propertyMapping.getNestedQueryId();
    // 获得属性名
    final String property = propertyMapping.getProperty();
    // 获得内嵌查询的 MappedStatement 对象
    final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
    // 获得内嵌查询的参数类型
    final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
    // 获得内嵌查询的参数对象
    final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rs, propertyMapping, nestedQueryParameterType, columnPrefix);
    Object value = null;
    if (nestedQueryParameterObject != null) {
      // 获得 BoundSql 对象
      final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
      // 获得 CacheKey 对象
      final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT, nestedBoundSql);
      final Class<?> targetType = propertyMapping.getJavaType();
      // 检查缓存中已存在
      if (executor.isCached(nestedQuery, key)) { //  有缓存
        // 创建 DeferredLoad 对象，并通过该 DeferredLoad 对象从缓存中加载结采对象
        executor.deferLoad(nestedQuery, metaResultObject, property, key, targetType);
        // 返回已定义
        value = DEFERED;
        // 检查缓存中不存在
      } else { // 无缓存
        // 创建 ResultLoader 对象
        final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, key, nestedBoundSql);
        // 如果要求延迟加载，则延迟加载
        if (propertyMapping.isLazy()) {
          // 如果该属性配置了延迟加载，则将其添加到 `ResultLoader.loaderMap` 中，等待真正使用时再执行嵌套查询并得到结果对象。
          lazyLoader.addLoader(property, metaResultObject, resultLoader);
          // 返回已定义
          value = DEFERED;
          // 如果不要求延迟加载，则直接执行加载对应的值
        } else {
          value = resultLoader.loadResult();
        }
      }
    }
    return value;
  }

  /**
   * 获得内嵌查询的参数类型
   */
  private Object prepareParameterForNestedQuery(ResultSet rs, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
    if (resultMapping.isCompositeResult()) {
      return prepareCompositeKeyParameter(rs, resultMapping, parameterType, columnPrefix);
    } else {
      return prepareSimpleKeyParameter(rs, resultMapping, parameterType, columnPrefix);
    }
  }

  /**
   * 获得普通类型的内嵌查询的参数对象
   */
  private Object prepareSimpleKeyParameter(ResultSet rs, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
    // 获得 TypeHandler 对象
    final TypeHandler<?> typeHandler;
    if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
      typeHandler = typeHandlerRegistry.getTypeHandler(parameterType);
    } else {
      typeHandler = typeHandlerRegistry.getUnknownTypeHandler();
    }
    // 获得指定字段的值
    return typeHandler.getResult(rs, prependPrefix(resultMapping.getColumn(), columnPrefix));
  }

  /**
   * 获得组合类型的内嵌查询的参数对象
   */
  private Object prepareCompositeKeyParameter(ResultSet rs, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
    // 创建参数对象
    final Object parameterObject = instantiateParameterObject(parameterType);
    // 创建参数对象的 MetaObject 对象，可对其进行访问
    final MetaObject metaObject = configuration.newMetaObject(parameterObject);
    boolean foundValues = false;
    // 遍历组合的所有字段
    for (ResultMapping innerResultMapping : resultMapping.getComposites()) {
      // 获得属性类型
      final Class<?> propType = metaObject.getSetterType(innerResultMapping.getProperty());
      // 获得对应的 TypeHandler 对象
      final TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(propType);
      // 获得指定字段的值
      final Object propValue = typeHandler.getResult(rs, prependPrefix(innerResultMapping.getColumn(), columnPrefix));
      // issue #353 & #560 do not execute nested query if key is null
      // 设置到 parameterObject 中，通过 metaObject
      if (propValue != null) {
        metaObject.setValue(innerResultMapping.getProperty(), propValue);
        foundValues = true; // 标记 parameterObject 非空对象
      }
    }
    // 返回参数对象
    return foundValues ? parameterObject : null;
  }

  /**
   * 创建参数对象
   */
  private Object instantiateParameterObject(Class<?> parameterType) {
    if (parameterType == null) {
      return new HashMap<>();
    } else if (ParamMap.class.equals(parameterType)) {
      return new HashMap<>(); // issue #649
    } else {
      return objectFactory.create(parameterType);
    }
  }

  /**
   * 根据该行记录以及 ResultMap.discriminator ，决定映射使用的 ResultMap 对象。最终拿到的是某个 case 的 resultMap
   * 鉴别器使用示例：
   * <discriminator column="personType" javaType="String">
   *      <case value="EmployeeType">
   *          <discriminator column="employeeType" javaType="String">
   *              <case value="DirectorType" resultMap="directorMap"/>
   *          </discriminator>
   *      </case>
   * </discriminator>
   */
  public ResultMap resolveDiscriminatedResultMap(ResultSet rs, ResultMap resultMap, String columnPrefix) throws SQLException {
    // 记录已经处理过的 Discriminator 对应的 ResultMap 的编号
    Set<String> pastDiscriminators = new HashSet<>();
    // 如果存在 Discriminator 对象，则基于其获得 ResultMap 对象
    Discriminator discriminator = resultMap.getDiscriminator();
    // 因为 Discriminator 可以嵌套 Discriminator ，所以是一个递归的过程。通过示例很好理解
    while (discriminator != null) {
      // 获得 Discriminator 的指定字段，在 ResultSet 中该字段的值
      final Object value = getDiscriminatorValue(rs, discriminator, columnPrefix);
      // 从 Discriminator 获取该值对应的 ResultMap 的编号。就是 case 的 resultMap
      final String discriminatedMapId = discriminator.getMapIdFor(String.valueOf(value));
      // 如果存在，则使用该 ResultMap 对象。TODO 如👆的使用示例，外层的case是没有对应的显示的resultMap，也会有resultMap注册到configuration吗
      if (configuration.hasResultMap(discriminatedMapId)) {
        // 获得该 ResultMap 对象
        resultMap = configuration.getResultMap(discriminatedMapId);
        // 判断，如果出现“重复”的情况，结束循环
        Discriminator lastDiscriminator = discriminator;
        discriminator = resultMap.getDiscriminator();
        if (discriminator == lastDiscriminator || !pastDiscriminators.add(discriminatedMapId)) {
          break;
        }
        // 如果不存在，直接结束循环
      } else {
        break;
      }
    }
    return resultMap;
  }

  /**
   * 获得 ResultSet 的指定字段的值
   *
   * @param rs ResultSet 对象
   * @param discriminator Discriminator 对象
   * @param columnPrefix 字段名的前缀
   * @return 指定字段的值
   */
  private Object getDiscriminatorValue(ResultSet rs, Discriminator discriminator, String columnPrefix) throws SQLException {
    // 获取 discriminator 对应的 resultMapping
    final ResultMapping resultMapping = discriminator.getResultMapping();
    // 获取 指定的类型处理器
    final TypeHandler<?> typeHandler = resultMapping.getTypeHandler();
    // 获得 ResultSet 的指定字段的值
    return typeHandler.getResult(rs, prependPrefix(resultMapping.getColumn(), columnPrefix));
  }

  /**
   * 拼接指定字段的前缀
   *
   * @param columnName 字段的名字
   * @param prefix 前缀
   * @return prefix + columnName
   */
  private String prependPrefix(String columnName, String prefix) {
    if (columnName == null || columnName.length() == 0 || prefix == null || prefix.length() == 0) {
      return columnName;
    }
    return prefix + columnName;
  }

  /**
   * 根据最终确定的 ResultMap 对 ResultSet 中的该行记录进行映射，得到映射后的结果对象
   */
  private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, String columnPrefix, Object partialObject) throws SQLException {
    // 获取 resultMapId
    final String resultMapId = resultMap.getId();
    // 先记录 rowValue 为 partialObject
    Object rowValue = partialObject;
    if (rowValue != null) {
      // 拿到 rowValue 的元信息
      final MetaObject metaObject = configuration.newMetaObject(rowValue);
      // 添加到 ancestorObjects
      putAncestor(rowValue, resultMapId);
      //
      applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, false);
      ancestorObjects.remove(resultMapId);
    } else {
      final ResultLoaderMap lazyLoader = new ResultLoaderMap();
      rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
      if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
        final MetaObject metaObject = configuration.newMetaObject(rowValue);
        boolean foundValues = this.useConstructorMappings;
        if (shouldApplyAutomaticMappings(resultMap, true)) {
          foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
        }
        foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
        putAncestor(rowValue, resultMapId);
        foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues;
        ancestorObjects.remove(resultMapId);
        foundValues = lazyLoader.size() > 0 || foundValues;
        rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
      }
      if (combinedKey != CacheKey.NULL_CACHE_KEY) {
        nestedResultObjects.put(combinedKey, rowValue);
      }
    }
    return rowValue;
  }

  /**
   * 添加到 ancestorObjects
   */
  private void putAncestor(Object resultObject, String resultMapId) {
    ancestorObjects.put(resultMapId, resultObject);
  }

  /**
   * 针对内嵌的RESULT MAP (比如join查询)
   */
  private boolean applyNestedResultMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String parentPrefix, CacheKey parentRowKey, boolean newObject) {
    // 先标记 foundValues 为false，
    boolean foundValues = false;
    // 遍历 resultMapping
    for (ResultMapping resultMapping : resultMap.getPropertyResultMappings()) {
      // 拿到resultMapping 内嵌的 nestedResultMapId
      final String nestedResultMapId = resultMapping.getNestedResultMapId();
      // nestedResultMapId 不为空说明，那一列确实可能有join操作，后面的resultMapping.getResultSet()代表不是存储过程
      if (nestedResultMapId != null) {
        try {
          // 拿到完整的前缀
          final String columnPrefix = getColumnPrefix(parentPrefix, resultMapping);
          // 处理下鉴别器的场景
          final ResultMap nestedResultMap = getNestedResultMap(rsw.getResultSet(), nestedResultMapId, columnPrefix);
          // 当没有指定列前缀时，需要去解决循环引用的问题，可以看看 issue #215
          if (resultMapping.getColumnPrefix() == null) {
            // 拿到上一级的对象，也就是没有填充关联对象的值
            Object ancestorObject = ancestorObjects.get(nestedResultMapId);
            // 如果不为空
            if (ancestorObject != null) {
              // 如果是新的对象
              if (newObject) {
                // 将 ancestorObject 的值设置到 metaObject 里，可以看看 issue #385
                linkObjects(metaObject, resultMapping, ancestorObject);
              }
              continue;
            }
          }
          // 创建缓存键，就是标记一条结果的唯一性
          final CacheKey rowKey = createRowKey(nestedResultMap, rsw, columnPrefix);
          final CacheKey combinedKey = combineKeys(rowKey, parentRowKey);
          Object rowValue = nestedResultObjects.get(combinedKey);
          boolean knownValue = rowValue != null;
          instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject); // mandatory
          if (anyNotNullColumnHasValue(resultMapping, columnPrefix, rsw)) {
            rowValue = getRowValue(rsw, nestedResultMap, combinedKey, columnPrefix, rowValue);
            if (rowValue != null && !knownValue) {
              linkObjects(metaObject, resultMapping, rowValue);
              foundValues = true;
            }
          }
        } catch (SQLException e) {
          throw new ExecutorException("Error getting nested result map values for '" + resultMapping.getProperty() + "'.  Cause: " + e, e);
        }
      }
    }
    return foundValues;
  }

  /**
   * 拼接拿到完整的前缀，包括上一级的前缀和这一级的前缀
   */
  private String getColumnPrefix(String parentPrefix, ResultMapping resultMapping) {
    final StringBuilder columnPrefixBuilder = new StringBuilder();
    if (parentPrefix != null) {
      columnPrefixBuilder.append(parentPrefix);
    }
    if (resultMapping.getColumnPrefix() != null) {
      columnPrefixBuilder.append(resultMapping.getColumnPrefix());
    }
    // 拼接后要转大写哦
    return columnPrefixBuilder.length() == 0 ? null : columnPrefixBuilder.toString().toUpperCase(Locale.ENGLISH);
  }

  private boolean anyNotNullColumnHasValue(ResultMapping resultMapping, String columnPrefix, ResultSetWrapper rsw) throws SQLException {
    Set<String> notNullColumns = resultMapping.getNotNullColumns();
    if (notNullColumns != null && !notNullColumns.isEmpty()) {
      ResultSet rs = rsw.getResultSet();
      for (String column : notNullColumns) {
        rs.getObject(prependPrefix(column, columnPrefix));
        if (!rs.wasNull()) {
          return true;
        }
      }
      return false;
    } else if (columnPrefix != null) {
      for (String columnName : rsw.getColumnNames()) {
        if (columnName.toUpperCase().startsWith(columnPrefix.toUpperCase())) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * 拿到内嵌的 nestedResultMap
   */
  private ResultMap getNestedResultMap(ResultSet rs, String nestedResultMapId, String columnPrefix) throws SQLException {
    // 首先从configuration中拿到该 ResultMap
    ResultMap nestedResultMap = configuration.getResultMap(nestedResultMapId);
    return resolveDiscriminatedResultMap(rs, nestedResultMap, columnPrefix);
  }

  /**
   * 创建缓存键，就是标记一条结果的唯一性
   */
  private CacheKey createRowKey(ResultMap resultMap, ResultSetWrapper rsw, String columnPrefix) throws SQLException {
    // 初始化
    final CacheKey cacheKey = new CacheKey();
    // 添加 resultMapID 到 cacheKey
    cacheKey.update(resultMap.getId());
    // 拿到所有的ID标签或者属性标签
    List<ResultMapping> resultMappings = getResultMappingsForRowKey(resultMap);
    if (resultMappings.isEmpty()) {
      // 依然为空？额，确实有可能，比如类型就是map，没有属性啊对不对
      if (Map.class.isAssignableFrom(resultMap.getType())) {
        // 针对map类型的
        createRowKeyForMap(rsw, cacheKey);
      } else {
        // 针对其他类型的，因为 resultMappings 是空的，因此将所有没映射到的字段值写到 cacheKey
        createRowKeyForUnmappedProperties(resultMap, rsw, cacheKey, columnPrefix);
      }
    } else {
      // 不为空，将映射的字段写到 cacheKey
      createRowKeyForMappedProperties(resultMap, rsw, cacheKey, resultMappings, columnPrefix);
    }
    // 更新次数为1，也就是只写入了 'resultMap.getId()'，那就是啥也没写，使用空缓存
    if (cacheKey.getUpdateCount() < 2) {
      return CacheKey.NULL_CACHE_KEY;
    }
    return cacheKey;
  }

  private CacheKey combineKeys(CacheKey rowKey, CacheKey parentRowKey) {
    if (rowKey.getUpdateCount() > 1 && parentRowKey.getUpdateCount() > 1) {
      CacheKey combinedKey;
      try {
        combinedKey = rowKey.clone();
      } catch (CloneNotSupportedException e) {
        throw new ExecutorException("Error cloning cache key.  Cause: " + e, e);
      }
      combinedKey.update(parentRowKey);
      return combinedKey;
    }
    return CacheKey.NULL_CACHE_KEY;
  }

  /**
   * 拿到ResultMap 能代表唯一性的 ResultMapping 列表。有 ID 用ID，没ID用所有列
   */
  private List<ResultMapping> getResultMappingsForRowKey(ResultMap resultMap) {
    // 拿到所有的ID标签
    List<ResultMapping> resultMappings = resultMap.getIdResultMappings();
    // 为空的话，拿所有属性的标签。这里也说明了，ID标签保证唯一性，能提高效率，不然 CacheKey 的 equal 就需要判断所有列的值
    if (resultMappings.isEmpty()) {
      resultMappings = resultMap.getPropertyResultMappings();
    }
    return resultMappings;
  }

  /**
   * 将映射的字段写到 cacheKey
   */
  private void createRowKeyForMappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, List<ResultMapping> resultMappings, String columnPrefix) throws SQLException {
    // 遍历 resultMappings
    for (ResultMapping resultMapping : resultMappings) {
      if (resultMapping.getNestedResultMapId() != null) {
        // 针对有内嵌查询的场景，参见 Issue #392
        final ResultMap nestedResultMap = configuration.getResultMap(resultMapping.getNestedResultMapId());
        // 递归调用，将子结果也写进去，如果懒加载没有执行的话，结果是空，没啥吊用
        createRowKeyForMappedProperties(nestedResultMap, rsw, cacheKey, nestedResultMap.getConstructorResultMappings(),
            prependPrefix(resultMapping.getColumnPrefix(), columnPrefix));
      }
      // 没有内嵌查询的场景
      else if (resultMapping.getNestedQueryId() == null) {
        // 拿到前缀加身的列名
        final String column = prependPrefix(resultMapping.getColumn(), columnPrefix);
        // 拿到类型处理器
        final TypeHandler<?> th = resultMapping.getTypeHandler();
        // 拿到结果匹配的上的所有列
        List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
        // Issue #114
        if (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))) {
          final Object value = th.getResult(rsw.getResultSet(), column);
          if (value != null || configuration.isReturnInstanceForEmptyRow()) {
            // 记录结果
            cacheKey.update(column);
            cacheKey.update(value);
          }
        }
      }
    }
  }

  /**
   * 将所有没映射到的字段值写到 cacheKey
   */
  private void createRowKeyForUnmappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, String columnPrefix) throws SQLException {
    // 拿到返回类型的元信息
    final MetaClass metaType = MetaClass.forClass(resultMap.getType(), reflectorFactory);
    // 拿到所有匹配不上的列
    List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
    for (String column : unmappedColumnNames) {
      String property = column;
      if (columnPrefix != null && !columnPrefix.isEmpty()) {
        // 如果指定了前缀，移除后即为对应的property
        if (column.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
          property = column.substring(columnPrefix.length());
        } else {
          continue;
        }
      }
      // 如果类有指定字段
      if (metaType.findProperty(property, configuration.isMapUnderscoreToCamelCase()) != null) {
        // 拿到指定列的值，注意两点：
        // 1、使用的是column而不是property，也就是说ResultSet的结果是包含前缀的
        // 2、ResultSet可能有多条记录，每次getString(column)游标都会向下划一位，也就是说 cacheKey 存的是一条记录的缓存
        String value = rsw.getResultSet().getString(column);
        if (value != null) {
          cacheKey.update(column);
          cacheKey.update(value);
        }
      }
    }
  }

  /**
   * 针对map类型的返回值，将结果写到 缓存键
   */
  private void createRowKeyForMap(ResultSetWrapper rsw, CacheKey cacheKey) throws SQLException {
    // 拿到所有列名
    List<String> columnNames = rsw.getColumnNames();
    // 遍历列名
    for (String columnName : columnNames) {
      // 拿到值
      final String value = rsw.getResultSet().getString(columnName);
      // 设置到cacheKey
      if (value != null) {
        cacheKey.update(columnName);
        cacheKey.update(value);
      }
    }
  }

  /**
   *
   * TODO 三个属性值交代清除
   * @param resultMapping 映射关系
   * @param metaObject 元对象
   * @param rowValue 关联的记录？
   */
  private void linkObjects(MetaObject metaObject, ResultMapping resultMapping, Object rowValue) {
    // 如果元对象的指定属性是集合，获取值/初始化 返回，不是集合返回null
    final Object collectionProperty = instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject);
    // 不为null，说明属性类型确实是集合。也就是一对多
    if (collectionProperty != null) {
      // 拿到该属性值的元信息，也就是集合的元信息
      final MetaObject targetMetaObject = configuration.newMetaObject(collectionProperty);
      // 添加进去
      targetMetaObject.add(rowValue);
    } else {
      // 不是null，说明不是属性，也就是一对一，将 rowValue 赋值给该属性
      metaObject.setValue(resultMapping.getProperty(), rowValue);
    }
  }

  /**
   * 如果属性类型是集合的话，返回该值，为空的话初始化一个集合，设置进去，并返回
   */
  private Object instantiateCollectionPropertyIfAppropriate(ResultMapping resultMapping, MetaObject metaObject) {
    // 获取字段名
    final String propertyName = resultMapping.getProperty();
    // 获取字段值
    Object propertyValue = metaObject.getValue(propertyName);
    // 如果值为空
    if (propertyValue == null) {
      // 从 resultMapping 中获取字段的Java类型
      Class<?> type = resultMapping.getJavaType();
      if (type == null) {
        // 没拿到，就通过对象元信息，拿到类型
        type = metaObject.getSetterType(propertyName);
      }
      try {
        // 如果是集合
        if (objectFactory.isCollection(type)) {
          // 初始化一个集合
          propertyValue = objectFactory.create(type);
          // 设置到元对象
          metaObject.setValue(propertyName, propertyValue);
          // 返回
          return propertyValue;
        }
      } catch (Exception e) {
        throw new ExecutorException("Error instantiating collection property for result '" + resultMapping.getProperty() + "'.  Cause: " + e, e);
      }
    } else if (objectFactory.isCollection(propertyValue.getClass())) {
      // 字段值不为空，但是是集合类型，返回该值
      return propertyValue;
    }
    // 不为空，又不是结合，返回 null
    return null;
  }

  /**
   * 判断是否结果对象是否有 TypeHandler 对象
   */
  private boolean hasTypeHandlerForResultObject(ResultSetWrapper rsw, Class<?> resultType) {
    // 如果返回的字段只有一个，则直接判断该字段是否有 TypeHandler 对象
    if (rsw.getColumnNames().size() == 1) {
      return typeHandlerRegistry.hasTypeHandler(resultType, rsw.getJdbcType(rsw.getColumnNames().get(0)));
    }
    // 判断 resultType 是否有对应的 TypeHandler 对象
    return typeHandlerRegistry.hasTypeHandler(resultType);
  }

}
