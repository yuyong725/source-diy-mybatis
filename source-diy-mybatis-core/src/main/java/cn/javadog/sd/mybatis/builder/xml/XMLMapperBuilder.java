package cn.javadog.sd.mybatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.javadog.sd.mybatis.builder.BaseBuilder;
import cn.javadog.sd.mybatis.builder.MapperBuilderAssistant;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.mapping.Discriminator;
import cn.javadog.sd.mybatis.mapping.ParameterMapping;
import cn.javadog.sd.mybatis.mapping.ParameterMode;
import cn.javadog.sd.mybatis.mapping.ResultFlag;
import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.mapping.ResultMapping;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.exceptions.IncompleteElementException;
import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.parsing.XNode;
import cn.javadog.sd.mybatis.support.parsing.XPathParser;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;

/**
 * @author 余勇
 * @date 2019-12-12 19:15
 * 加载 Mapper 映射配置文件 入口
 * 继承 BaseBuilder 抽象类，Mapper XML 配置构建器，主要负责解析 Mapper 映射配置文件。
 */
public class XMLMapperBuilder extends BaseBuilder {

  /**
   * 基于 Java XPath 解析器
   */
  private final XPathParser parser;

  /**
   * Mapper 构造器助手
   * 是 XMLMapperBuilder 和 MapperAnnotationBuilder 的小助手，提供了一些公用的方法，例如创建 ParameterMap、MappedStatement 对象等等
   */
  private final MapperBuilderAssistant builderAssistant;

  /**
   * 可被其他语句引用的可重用语句块的集合
   *
   * 例如：<sql id="userColumns"> ${alias}.id,${alias}.username,${alias}.password </sql>
   */
  private final Map<String, XNode> sqlFragments;

  /**
   * 资源引用的地址，也就是mapper.xml的地址
   */
  private final String resource;

  /**
   * 构造
   */
  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    // 将命名空间赋值给 builderAssistant。note 这里没设置，解析mapper的时候也会设置的，因此不用担心builderAssistant调用方法出现GG
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  /**
   * 构造
   */
  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, configuration.getVariables()), configuration, resource, sqlFragments);
  }

  /**
   * 构造
   */
  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  /**
   * 构造
   */
  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, configuration.getVariables()), configuration, resource, sqlFragments);
  }

  /**
   * 构造的最终目的地
   */
  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    super(configuration);
    // 创建 MapperBuilderAssistant 对象
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  /**
   * 解析 Mapper XML 配置文件
   */
  public void parse() {
    // 判断当前 Mapper 是否已经加载过
    if (!configuration.isResourceLoaded(resource)) {
      // 解析 `<mapper />` 节点
      configurationElement(parser.evalNode("/mapper"));
      // 标记该 Mapper 已经加载过
      configuration.addLoadedResource(resource);
      // 绑定 Mapper，
      // note 这里也会调用configuration.addLoadedResource，这里加进去是 'namespace:***'，防止的是与
      //  MapperAnnotationBuilder#loadXmlResource冲突。上面加进去的是 '***Mapper.xml'，防止的是与 其他的xml重复加载
      bindMapperForNamespace();
    }
    // 解析待定的 <resultMap /> 节点，就是之前解析失败的
    parsePendingResultMaps();
    // 解析待定的 <cache-ref /> 节点，就是之前解析失败的
    parsePendingCacheRefs();
    // 解析待定的 SQL 语句的节点，就是之前解析失败的
    parsePendingStatements();
  }

  /**
   * 获取指定refid对应的SQL节点
   */
  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  /**
   * 解析 <mapper /> 节点
   */
  private void configurationElement(XNode context) {
    try {
      // 获得 namespace 属性
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        // 不可为空，为空直接GG
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      builderAssistant.setCurrentNamespace(namespace);
      // 解析 <cache-ref /> 节点
      cacheRefElement(context.evalNode("cache-ref"));
      // 解析 <cache /> 节点
      cacheElement(context.evalNode("cache"));
      // 已废弃！老式风格的参数映射。内联参数是首选, 这个元素可能在将来被移除，这里不会记录。
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      // 解析 <resultMap /> 节点们
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      // 解析 <sql /> 节点们
      sqlElement(context.evalNodes("/mapper/sql"));
      // 解析 <select /> <insert /> <update /> <delete /> 节点们
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  /**
   * 解析所有 <select />、<insert />、<update />、<delete /> 节点
   * 完全可以简写成一行：buildStatementFromContext(list, configuration.getDatabaseId());
   */
  private void buildStatementFromContext(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    buildStatementFromContext(list, null);
  }

  /**
   * 解析所有 <select />、<insert />、<update />、<delete /> 节点
   */
  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    // 遍历 <select /> <insert /> <update /> <delete /> 节点们
    for (XNode context : list) {
      // 获得 XMLStatementBuilder
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
        // 进行解析
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        // 解析失败，添加到 configuration 中
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  /**
   * 解析之前解析失败的ResultMap
   */
  private void parsePendingResultMaps() {
    // 获得解析失败的 ResultMapResolver 集合，并遍历进行处理
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    // 加同步，TODO 之前解析是因为同步的问题吗？
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          // 执行解析
          iter.next().resolve();
          // 移除
          iter.remove();
        } catch (IncompleteElementException e) {
          // 解析失败，不抛出异常，但也不在解析了
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  /**
   * 解析之前解析失败的CacheRef
   */
  private void parsePendingCacheRefs() {
    // 获得 CacheRefResolver 集合，并遍历进行处理
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          // 执行解析
          iter.next().resolveCacheRef();
          // 移除
          iter.remove();
        } catch (IncompleteElementException e) {
          // 解析失败，不抛出异常，但也不在解析了
          // Cache ref is still missing a resource...
        }
      }
    }
  }

  /**
   * 解析之前解析失败的SQL 语句的节点
   */
  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          // 执行解析
          iter.next().parseStatementNode();
          // 移除
          iter.remove();
        } catch (IncompleteElementException e) {
          // 解析失败，不抛出异常，但也不在解析了
          // Statement is still missing a resource...
        }
      }
    }
  }

  /**
   * 解析 <cache-ref /> 节点
   */
  private void cacheRefElement(XNode context) {
    if (context != null) {
      // 获得指向的 namespace 名字，并添加到 configuration 的 cacheRefMap 中
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      // 创建 CacheRefResolver 对象
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        // 执行解析。但解析的具体逻辑更像是只进行的指向缓存的校验，保存逻辑在上方的 configuration.addCacheRef
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        // 解析失败，添加到 configuration 的 incompleteCacheRefs 中
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  /**
   * 解析 <cache /> 标签
   */
  private void cacheElement(XNode context) throws Exception {
    if (context != null) {
      // 获得负责存储的 Cache 实现类别名，默认是 PERPETUAL
      String type = context.getStringAttribute("type", "PERPETUAL");
      // 拿到别名对应的实现类
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      // 获得缓存的过期策略，默认是LRU
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      // 获得 flushInterval、size、readWrite、blocking 属性
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      // 注意这里取反。readOnly 代表只读，readWrite 代表可读写
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      // 获得 Properties 属性
      Properties props = context.getChildrenAsProperties();
      // 创建 Cache 对象
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  /**
   * 解析parameterMap，逻辑很简单
   */
  private void parameterMapElement(List<XNode> list) throws Exception {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        @SuppressWarnings("unchecked")
        Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
        // 构建ParameterMapping
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      // 构建ParameterMap
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  /**
   * 解析所有 <resultMap /> 节点
   */
  private void resultMapElements(List<XNode> list) throws Exception {
    // 遍历 <resultMap /> 节点们
    for (XNode resultMapNode : list) {
      try {
        // 处理单个 <resultMap /> 节点
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // 忽略，后面会统一重试的
      }
    }
  }

  /**
   * 解析 <resultMap /> 节点
   */
  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.<ResultMapping> emptyList());
  }

  /**
   * 解析 <resultMap /> 节点
   *
   * @param resultMapNode 要解析的节点
   * @param additionalResultMappings 要额外添加的ResultMapping，针对 TODO 啥场景忘了
   */
  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    // 获得 id 属性
    String id = resultMapNode.getStringAttribute("id", resultMapNode.getValueBasedIdentifier());
    // 获得 type 属性，没有就拿 ofType，再没有就拿 resultType， 再没有就拿 javaType。note 因为这个节点可能是n种类型之一
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    // 获得 extends 属性
    String extend = resultMapNode.getStringAttribute("extends");
    // 获得 autoMapping 属性
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    // 解析 type 对应的类
    Class<?> typeClass = resolveClass(type);
    //
    Discriminator discriminator = null;
    // 创建 ResultMapping 集合
    List<ResultMapping> resultMappings = new ArrayList<>();
    // 先将额外要加的ResultMapping加进去
    resultMappings.addAll(additionalResultMappings);
    // 遍历 <resultMap /> 的子节点
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      // 处理 <constructor /> 节点，它的作用是，代替默认的构造
      if ("constructor".equals(resultChild.getName())) {
        processConstructorElement(resultChild, typeClass, resultMappings);
      }
      // 处理 <discriminator /> 节点
      else if ("discriminator".equals(resultChild.getName())) {
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      }
      // 处理其它节点
      else {
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    // 创建 ResultMapResolver 对象，执行解析
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      // 构建ResultMap，添加到configuration
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      // 解析失败，添加到 configuration 中
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  /**
   * 处理 <constructor /> 节点
   */
  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    // 遍历 <constructor /> 的子节点们
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      // 获得 ResultFlag 集合
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        // note 这里解释了为什么 ResultMapping 的flags属性是数组，因为当是<constructor />字节点时，都会加上ResultFlag.CONSTRUCTOR，而本身还可能是ResultFlag.ID
        flags.add(ResultFlag.ID);
      }
      // 将当前子节点构建成 ResultMapping 对象，并添加到 resultMappings 中
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  /**
   * 处理 <discriminator /> 节点
   */
  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    // 解析各种属性
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    // 解析各种属性对应的类
    Class<?> javaTypeClass = resolveClass(javaType);
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    // 遍历 <discriminator /> 的子节点，解析成 discriminatorMap 集合，其字节点就一种类型 <case />
    Map<String, String> discriminatorMap = new HashMap<>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
      discriminatorMap.put(value, resultMap);
    }
    // 创建 Discriminator 对象
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  /**
   * 解析所有 <sql /> 节点
   * note 完全可以简写成 sqlElement(list, configuration.getDatabaseId());
   */
  private void sqlElement(List<XNode> list) throws Exception {
    if (configuration.getDatabaseId() != null) {
      // 如果有数据库标示的话，解析时要多加验证
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }

  /**
   * 解析所有 <sql /> 节点
   *
   * @param list 要解析的SQL节点
   * @param requiredDatabaseId 数据库标示
   */
  private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
    // 遍历所有 <sql /> 节点
    for (XNode context : list) {
      // 获得 databaseId 属性
      String databaseId = context.getStringAttribute("databaseId");
      // 获得完整的 id 属性，格式为 `${namespace}.${id}`
      String id = context.getStringAttribute("id");
      id = builderAssistant.applyCurrentNamespace(id, false);
      // 判断 databaseId 是否匹配
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        // 添加到 sqlFragments 中
        sqlFragments.put(id, context);
      }
    }
  }

  /**
   * 判断 databaseId 是否匹配
   *
   * @param id SQL id
   */
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    // requiredDatabaseId 不为空代表需要校验
    if (requiredDatabaseId != null) {
      // 不相等，说明不匹配
      if (!requiredDatabaseId.equals(databaseId)) {
        return false;
      }
    } else {
      // 如果未设置 requiredDatabaseId ，但是 databaseId 存在，说明还是不匹配，则返回 false
      if (databaseId != null) {
        return false;
      }
      // 判断是否已经存在，TODO 同一个节点被多次加载？
      if (this.sqlFragments.containsKey(id)) {
        XNode context = this.sqlFragments.get(id);
        // 若存在，则判断原有的 sqlFragment 是否 databaseId 为空。因为，当前 databaseId 为空，这样两者才能匹配。
        if (context.getStringAttribute("databaseId") != null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * 将当前节点构建成 ResultMapping 对象
   */
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
    // 获得各种属性
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      // ResultFlag.CONSTRUCTOR 这种的字段名使用的属性是"name"， 如 <idArg name="id" column="id" javaType="_int" />
      property = context.getStringAttribute("name");
    } else {
      // 其他的使用的是 "property"，如<result property="username" column="username" />
      property = context.getStringAttribute("property");
    }
    // 获取 column 属性
    String column = context.getStringAttribute("column");
    // 获取 javaType 属性
    String javaType = context.getStringAttribute("javaType");
    // 获取 jdbcType 属性
    String jdbcType = context.getStringAttribute("jdbcType");
    // 获取 nestedSelect 属性
    String nestedSelect = context.getStringAttribute("select");
    // 获取 nestedResultMap 属性
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.<ResultMapping> emptyList()));
    // 获取 notNullColumn 属性
    String notNullColumn = context.getStringAttribute("notNullColumn");
    // 获取 columnPrefix 属性
    String columnPrefix = context.getStringAttribute("columnPrefix");
    // 获取 typeHandler 属性
    String typeHandler = context.getStringAttribute("typeHandler");
    // 获取 resultSet 属性
    String resultSet = context.getStringAttribute("resultSet");
    // 获取 foreignColumn 属性
    String foreignColumn = context.getStringAttribute("foreignColumn");
    // 获取 lazy 属性，没配置的话就取 全局的
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    // 获得javaType对应的类
    Class<?> javaTypeClass = resolveClass(javaType);
    // 获得typeHandler对应的类
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
    // 获取jdbcType对应的类型
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    // 构建 ResultMapping 对象
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  /**
   * 处理内嵌的 ResultMap 的情况，返回该 ResultMap 对应的 ID。
   */
  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
    // 针对 association 或 collection 或 case 的场景, 这三者才会有内嵌的 ResultMappings
    if ("association".equals(context.getName())
        || "collection".equals(context.getName())
        || "case".equals(context.getName())) {
      // 如果没有 关联的 select 属性，就去解析该节点。有select节点，说明需要懒加载的方式去触发，这里不管的
      if (context.getStringAttribute("select") == null) {
        // 解析，并返回 ResultMap
        ResultMap resultMap = resultMapElement(context, resultMappings);
        return resultMap.getId();
      }
    }
    return null;
  }

  /**
   * 绑定 Mapper
   */
  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      // 获得 Mapper 映射配置文件对应的 Mapper 接口，实际上类名就是 namespace 。嘿嘿，这个是常识。
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        // 忽略，这里没找到不要紧，也就是说你可以写一个没有对应mapper接口的xml
      }
      // 找到了对应的mapper，那就去绑定
      if (boundType != null) {
        // 不存在该 Mapper 接口，则进行添加
        if (!configuration.hasMapper(boundType)) {
          // 标记 namespace 已经添加，避免 MapperAnnotationBuilder#loadXmlResource(...) 重复加载
          configuration.addLoadedResource("namespace:" + namespace);
          // 添加到 configuration 中
          configuration.addMapper(boundType);
        }
      }
    }
  }

}
