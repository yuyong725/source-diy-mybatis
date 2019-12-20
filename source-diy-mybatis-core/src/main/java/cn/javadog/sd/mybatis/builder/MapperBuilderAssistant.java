package cn.javadog.sd.mybatis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.mapping.CacheBuilder;
import cn.javadog.sd.mybatis.mapping.Discriminator;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ParameterMap;
import cn.javadog.sd.mybatis.mapping.ResultFlag;
import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.mapping.ResultMapping;
import cn.javadog.sd.mybatis.mapping.ResultSetType;
import cn.javadog.sd.mybatis.mapping.SqlCommandType;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.mapping.StatementType;
import cn.javadog.sd.mybatis.scripting.LanguageDriver;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.cache.decorators.LruCache;
import cn.javadog.sd.mybatis.support.cache.impl.PerpetualCache;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.support.exceptions.IncompleteElementException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaClass;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;

/**
 * @author 余勇
 * @date 2019-12-11 16:09
 * 继承 BaseBuilder 抽象类，Mapper 构造器的小助手
 * 提供了一些公用的方法，例如创建 ParameterMap、MappedStatement 对象等等。
 *
 * 与namespace一一对应
 *
 * note 务必看完这篇文章：https://mybatis.org/mybatis-3/zh/sqlmap-xml.html。因为很多较少使用的功能，不看文档根本不知道
 */
public class MapperBuilderAssistant extends BaseBuilder {

  /**
   * 当前 Mapper 命名空间
   */
  private String currentNamespace;

  /**
   * 资源引用的地址
   */
  private final String resource;

  /**
   * 当前 Cache 对象
   */
  private Cache currentCache;

  /**
   * 是否未解析成功 CacheRef
   * issue #676
   */
  private boolean unresolvedCacheRef;

  /**
   * 构造函数
   */
  public MapperBuilderAssistant(Configuration configuration, String resource) {
    // 父类构造走一波
    super(configuration);
    // 全局异常记录一笔
    ErrorContext.instance().resource(resource);
    // 记录下资源的地址，如 "org/apache/ibatis/builder/BlogMapper.xml"
    this.resource = resource;
  }

  /**
   * 获取当前的命名空间 currentNamespace
   */
  public String getCurrentNamespace() {
    return currentNamespace;
  }

  /**
   * 设置当前的命名空间 currentNamespace 属性
   */
  public void setCurrentNamespace(String currentNamespace) {
    // 如果传入的 currentNamespace 参数为空，抛出 BuilderException 异常
    if (currentNamespace == null) {
      throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
    }

    // 如果当前已经设置，并且还和传入的不相等，抛出 BuilderException 异常
    if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
      throw new BuilderException("Wrong namespace. Expected '"
          + this.currentNamespace + "' but found '" + currentNamespace + "'.");
    }

    this.currentNamespace = currentNamespace;
  }

  /**
   * 拼接命名空间,TODO 相关不相关哪来的
   * @param base 要拼接的名称，比如resultMap或parameterMap的ID
   * @param isReference 是否与当前命名空间相关
   */
  public String applyCurrentNamespace(String base, boolean isReference) {
    if (base == null) {
      return null;
    }
    if (isReference) {
      // 如果与当前命名空间相关，并验证了base包含'.'，那么就直接返回base
      if (base.contains(".")) {
        return base;
      }
    }
    // 与当前命名空间不相关
    else {
      // 如果是以当前命名空间+'.'开头，那也直接返回base，可以认为它就是与当前命名空间相关的
      if (base.startsWith(currentNamespace + ".")) {
        return base;
      }
      // 如果不是以当前命名空间+'.'开头，里面又包含'.'，可能是其他命名空间的，直接呵呵
      if (base.contains(".")) {
        throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
      }
    }
    // 走到最后可能的场景是：base里面没有'.'，不管是否与当前命名空间相关
    return currentNamespace + "." + base;
  }

  /**
   * 使用namespace指向的 Cache 对象。如果获得不到，则抛出 IncompleteElementException 异常
   * 也就是通过 <cache-ref />或者@cacheRef，去拿到另一个命名空间的缓存配置
   */
  public Cache useCacheRef(String namespace) {
    if (namespace == null) {
      throw new BuilderException("cache-ref element requires a namespace attribute.");
    }
    try {
      // 标记未解决
      unresolvedCacheRef = true;
      // 获得 Cache 对象
      Cache cache = configuration.getCache(namespace);
      // 获得不到，抛出 IncompleteElementException 异常
      if (cache == null) {
        throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
      }
      // 记录当前 Cache 对象
      currentCache = cache;
      // 标记已解决
      unresolvedCacheRef = false;
      return cache;
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
    }
  }

  /**
   * 创建 Cache 对象
   *
   * @param typeClass 缓存实现类
   * @param evictionClass 过期策略
   * @param blocking 是否阻塞，就是读取的时候是否阻塞其他线程读取
   * @param flushInterval 刷新周期
   * @param readWrite 是否可序列化
   * @param size 如果缓存实现类有size属性的话，就使用该值，这不是可存储缓存的大小
   * @param props 缓存实现类的特定字段的属性值
   */
  public Cache useNewCache(Class<? extends Cache> typeClass,
      Class<? extends Cache> evictionClass,
      Long flushInterval,
      Integer size,
      boolean readWrite,
      boolean blocking,
      Properties props) {
    // 创建 Cache 对象
    Cache cache = new CacheBuilder(currentNamespace)
        // 默认实现是 PerpetualCache
        .implementation(valueOrDefault(typeClass, PerpetualCache.class))
        // 默认过期策略是 LruCache
        .addDecorator(valueOrDefault(evictionClass, LruCache.class))
        // 默认不设置,也就是没有刷新间隔,缓存仅仅调用clear语句时刷新。设置了即使用ScheduledCache
        .clearInterval(flushInterval)
        // 如果缓存实现类有size属性的话，就使用该值，这不是可存储缓存的大小
        .size(size)
        // 是否可序列化缓存
        .readWrite(readWrite)
        // 缓存读取是否阻塞
        .blocking(blocking)
        // 阻塞的话就会包装 BlockingCache
        .properties(props)
        // 构建
        .build();
    // 添加到 configuration 的 caches 中
    configuration.addCache(cache);
    // 赋值给 currentCache
    currentCache = cache;
    return cache;
  }

  /**
   * 添加ResultMap
   *
   * @param id <resultMap /> 的id属性
   * @param type <resultMap /> 的type属性
   * @param autoMapping 如果设置这个属性，MyBatis将会为本结果映射开启或者关闭自动映射。 这个属性会覆盖全局的属性 autoMappingBehavior。默认值：未设置（unset）
   * @param discriminator(鉴别器) 使用结果值来决定使用哪个 resultMap，内嵌有 case 属性
   * @param resultMappings <resultMap /> 下的 <result /> 标签
   * @param extend <resultMap /> 下的extends 属性，用于补充当前的resultMap，这要求必须是当前 namespace 下。
   */
  public ResultMap addResultMap(
      String id,
      Class<?> type,
      String extend,
      Discriminator discriminator,
      List<ResultMapping> resultMappings,
      Boolean autoMapping) {
    // 获得 ResultMap 编号，即格式为 `${namespace}.${id}` 。
    id = applyCurrentNamespace(id, false);
    // 获取完整的 extend 属性，即格式为 `${namespace}.${extend}` 。从这里的逻辑来看，貌似只能自己 namespace 下的 ResultMap 。
    extend = applyCurrentNamespace(extend, true);

    // 如果有extend，则将 extend 的 ResultMap 集合，添加到 resultMappings 中。
    if (extend != null) {
      // 获得 extend 对应的 ResultMap 对象。如果不存在，则抛出 IncompleteElementException 异常
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
      }
      ResultMap resultMap = configuration.getResultMap(extend);
      // 获取 extend 的 ResultMap 对象的 ResultMapping 集合，note 之所以要new一个，因为要对这个list进行操作，而原则上，是不能更改extend对应的ResultMappings
      List<ResultMapping> extendedResultMappings = new ArrayList<>(resultMap.getResultMappings());
      // 移除 resultMappings
      extendedResultMappings.removeAll(resultMappings);
      // Remove parent constructor if this resultMap declares a constructor.
      // 判断当前的 resultMappings 是否有构造方法
      boolean declaresConstructor = false;
      for (ResultMapping resultMapping : resultMappings) {
        // resultMapping 下有 <constructor /> 标签，既代表有构造方法
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          declaresConstructor = true;
          break;
        }
      }
      // 如果有，则从 extendedResultMappings 移除所有的构造类型的 ResultMapping
      if (declaresConstructor) {
        Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
        while (extendedResultMappingsIter.hasNext()) {
          if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
            extendedResultMappingsIter.remove();
          }
        }
      }
      // 将 extendedResultMappings 添加到 resultMappings 中
      resultMappings.addAll(extendedResultMappings);
    }
    // 创建 ResultMap 对象
    ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
        .discriminator(discriminator)
        .build();
    // 添加到 configuration 中
    configuration.addResultMap(resultMap);
    // 返回resultMap
    return resultMap;
  }

  /**
   * 构建 Discriminator 鉴别器对象，用于针对不同的数据库返回结果，使用不同的resultMap或resultType
   * 使用方法如下：
   * <discriminator javaType="int" column="draft">
   *   <case value="1" resultType="DraftPost"/>
   * </discriminator>
   *
   * @param discriminatorMap key：case的value， value：case的resultType
   */
  public Discriminator buildDiscriminator(Class<?> resultType, String column, Class<?> javaType, JdbcType jdbcType,
      Class<? extends TypeHandler<?>> typeHandler, Map<String, String> discriminatorMap) {

    // 构建 ResultMapping 对象
    ResultMapping resultMapping = buildResultMapping(resultType, null, column, javaType,
        jdbcType, null, null, null, null, typeHandler,
        new ArrayList<>(), false);
    // 创建 namespaceDiscriminatorMap 映射
    Map<String, String> namespaceDiscriminatorMap = new HashMap<>();
    for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
      String resultMap = e.getValue();
      // 生成完整的 resultMap 标识
      resultMap = applyCurrentNamespace(resultMap, true);
      // 覆盖原来的
      namespaceDiscriminatorMap.put(e.getKey(), resultMap);
    }
    // 构建 Discriminator 对象
    return new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap).build();
  }

  /**
   * 构建 MappedStatement 对象
   * @param id MappedStatement的ID，对应SQL语句的ID
   */
  public MappedStatement addMappedStatement(
      String id,
      SqlSource sqlSource,
      StatementType statementType,
      SqlCommandType sqlCommandType,
      Integer fetchSize,
      Integer timeout,
      String parameterMap,
      Class<?> parameterType,
      String resultMap,
      Class<?> resultType,
      ResultSetType resultSetType,
      boolean flushCache,
      boolean useCache,
      boolean resultOrdered,
      KeyGenerator keyGenerator,
      String keyProperty,
      String keyColumn,
      LanguageDriver lang) {

    // 如果指向的 Cache 未解析，抛出 IncompleteElementException 异常。note 默认值是false，只有开始解析却未解析成功才会true
    if (unresolvedCacheRef) {
      throw new IncompleteElementException("Cache-ref not yet resolved");
    }

    // 获得 id 编号，格式为 `${namespace}.${id}`
    id = applyCurrentNamespace(id, false);
    // 是否是 查询语句
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;

    // 创建 MappedStatement.Builder 对象
    MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
        .resource(resource)
        .fetchSize(fetchSize)
        .timeout(timeout)
        .statementType(statementType)
        .keyGenerator(keyGenerator)
        .keyProperty(keyProperty)
        .keyColumn(keyColumn)
        .lang(lang)
        .resultOrdered(resultOrdered)
        // 获得 ResultMap 集合
        .resultMap(getStatementResultMap(resultMap, resultType, id))
        .resultSetType(resultSetType)
        .flushCacheRequired(valueOrDefault(flushCache, !isSelect))
        .useCache(valueOrDefault(useCache, isSelect))
        .cache(currentCache);

    // 获得 ParameterMap ，并设置到 MappedStatement.Builder 中
    ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
    if (statementParameterMap != null) {
      statementBuilder.parameterMap(statementParameterMap);
    }

    // 创建 MappedStatement 对象
    MappedStatement statement = statementBuilder.build();
    // 添加到 configuration 中
    configuration.addMappedStatement(statement);
    return statement;
  }

  /**
   * 获取非空的值，为空就用默认值
   */
  private <T> T valueOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  /**
   * 构建Statement的ParameterMap。如果是内联，对应的 ParameterMapping 是空数组
   */
  private ParameterMap getStatementParameterMap(String parameterMapName, Class<?> parameterTypeClass, String statementId) {
    parameterMapName = applyCurrentNamespace(parameterMapName, true);
    ParameterMap parameterMap = null;
    if (parameterMapName != null) {
      try {
        parameterMap = configuration.getParameterMap(parameterMapName);
      } catch (IllegalArgumentException e) {
        throw new IncompleteElementException("Could not find parameter map " + parameterMapName, e);
      }
    } else if (parameterTypeClass != null) {
      // 使用的是parameterType时，会加上"-Inline"，并且此种情况拿到的 ParameterMap 的 ParameterMapping 是空数组！
      parameterMap = new ParameterMap
          .Builder(configuration, statementId + "-Inline", parameterTypeClass, new ArrayList<>())
          .build();
    }
    return parameterMap;
  }

  /**
   * 获得 ResultMap，源码中是多个，这里不支持
   */
  private ResultMap getStatementResultMap(String resultMapName, Class<?> resultType, String statementId) {
    // 获得 resultMap 的编号
    resultMapName = applyCurrentNamespace(resultMapName, true);
    // 创建 ResultMap 集合
    ResultMap resultMap = null;
    // 如果 resultMap 非空，则获得 resultMap 对应的 ResultMap 对象(们）
    if (resultMapName != null) {
        try {
          // 从 configuration 中获得
          resultMap = configuration.getResultMap(resultMapName.trim());
        } catch (IllegalArgumentException e) {
          throw new IncompleteElementException("Could not find result map " + resultMapName, e);
        }
    }
    // 如果 resultType 非空，则创建 ResultMap 对象，对应的 ResultMapping 也是空数组
    else if (resultType != null) {
      // note 这里加的 '-Inline'，只有resultType才会出现这种
      resultMap = new ResultMap
          .Builder(configuration, statementId + "-Inline", resultType, new ArrayList<>(), null)
          .build();
    }
    return resultMap;
  }

  /**
   * 构造 ResultMapping 对象
   *
   * @param resultType <resultMap /> 标签上的type
   * @param property 对应的字段
   * @param column 对应的数据库表字段，当使用多个结果集时，该属性指定结果集中用于与 foreignColumn 匹配的列（多个列名以逗号隔开），以识别关系中的父类型与子类型。
   * @param javaType 字段的java类型
   * @param jdbcType 字段的jdbc类型
   * @param nestedSelect 嵌套的select语句的ID
   * @param nestedResultMap 嵌套关联的resultMap的ID
   * @param notNullColumn 默认情况下，在至少一个被映射到属性的列不为空时，子对象才会被创建。 你可以在这个属性上指定非空的列来改变默认行为，
   *  指定后，Mybatis 将只在这些列非空时才创建一个子对象。可以使用逗号分隔来指定多个列。
   *
   * @param columnPrefix 当连接多个表时，你可能会不得不使用列别名来避免在 ResultSet 中产生重复的列名。
   *  指定 columnPrefix 列名前缀允许你将带有这些前缀的列映射到一个外部的结果映射中。
   *
   * @param typeHandler 指定类型处理器
   * @param flags 对应的ResultFlag
   * @param lazy 是否懒加载
   */
  public ResultMapping buildResultMapping(
      Class<?> resultType,
      String property,
      String column,
      Class<?> javaType,
      JdbcType jdbcType,
      String nestedSelect,
      String nestedResultMap,
      String notNullColumn,
      String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler,
      List<ResultFlag> flags,
      boolean lazy) {

    // 解析对应的 Java Type 类和 TypeHandler 对象
    Class<?> javaTypeClass = resolveResultJavaType(resultType, property, javaType);
    // 解析 typeHandler 对应的 类型处理器实例，没找到的话就注册
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
    // 解析组合字段名称成 ResultMapping 集合。涉及「关联的嵌套查询」
    List<ResultMapping> composites = parseCompositeColumnName(column);
    // 创建 ResultMapping 对象
    return new ResultMapping.Builder(configuration, property, column, javaTypeClass)
        .jdbcType(jdbcType)
        .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
        .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true))
        .typeHandler(typeHandlerInstance)
        .flags(flags == null ? new ArrayList<ResultFlag>() : flags)
        .composites(composites)
        .notNullColumns(parseMultipleColumnNames(notNullColumn))
        .columnPrefix(columnPrefix)
        .lazy(lazy)
        .build();
  }

  /**
   * 将字符串解析成集合
   */
  private Set<String> parseMultipleColumnNames(String columnName) {
    Set<String> columns = new HashSet<>();
    if (columnName != null) {
      // 多个字段，使用 ，分隔
      if (columnName.indexOf(',') > -1) {
        StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
        while (parser.hasMoreTokens()) {
          String column = parser.nextToken();
          columns.add(column);
        }
      } else {
        columns.add(columnName);
      }
    }
    return columns;
  }

  /**
   * 解析组合字段名称成 ResultMapping 集合。
   * 这里必然多个字符串，如column="{prop1=col1,prop2=col2}"，用于关联的嵌套查询(该查询需要几个参数，这里就传几个)；
   */
  private List<ResultMapping> parseCompositeColumnName(String columnName) {
    List<ResultMapping> composites = new ArrayList<>();
    // 分词，解析其中的 property 和 column 的组合对
    if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
      // StringTokenizer 的分隔符不懂的自行百度，这里代表 '{', '}', '=', ' '都进行分割
      StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
      while (parser.hasMoreTokens()) {
        String property = parser.nextToken();
        String column = parser.nextToken();
        // 创建 ResultMapping 对象
        ResultMapping complexResultMapping = new ResultMapping.Builder(
            configuration, property, column,
            // 使用的UnknownTypeHandler，它会根据字段类型寻找最合适的处理器
            configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
        // 添加到 composites 中
        composites.add(complexResultMapping);
      }
    }
    return composites;
  }

  /**
   * 解析 <result />标签的字段的Java类型
   */
  private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
    if (javaType == null && property != null) {
      try {
        // 拿到类的元信息
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        // 获取字段的setter类型
        javaType = metaResultType.getSetterType(property);
      } catch (Exception e) {
        // 无视，👇的空检查就是处理这种
      }
    }
    // 没有合适的或者上面报错了，就直接使用 Object 类型
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  /**
   * 解析参数的Java类型。因为javaType本身可能为null，这时候就需要根据其他信息判断
   */
  private Class<?> resolveParameterJavaType(Class<?> parameterType, String property, Class<?> javaType, JdbcType jdbcType) {
    // javaType 为空时
    if (javaType == null) {
      if (JdbcType.CURSOR.equals(jdbcType)) {
        // jdbcType 是 CURSOR 的话，JavaType 使用 ResultSet 类型
        javaType = java.sql.ResultSet.class;
      } else if (Map.class.isAssignableFrom(parameterType)) {
        // 如果 parameterType 是 map的话，javaType 直接使用 Object 类型
        javaType = Object.class;
      } else {
        // 否则话，先拿到 parameterType 的元信息
        MetaClass metaResultType = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
        // 获取属性名的getter方法返回的类型
        javaType = metaResultType.getGetterType(property);
      }
    }
    // 如果还没有找到合适的话，就直接使用 Object 类型。TODO 为毛在这里还可能为null
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  /**
   * 获取指定语言的驱动
   */
  public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
    if (langClass != null) {
      // 注册
      configuration.getLanguageRegistry().register(langClass);
    } else {
      // 如果为空，则使用默认类
      langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
    }
    // 获得 LanguageDriver 对象
    return configuration.getLanguageRegistry().getDriver(langClass);
  }

}
