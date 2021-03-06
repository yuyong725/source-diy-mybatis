package cn.javadog.sd.mybatis.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.javadog.sd.mybatis.binding.MapperRegistry;
import cn.javadog.sd.mybatis.builder.annotation.MethodResolver;
import cn.javadog.sd.mybatis.builder.xml.CacheRefResolver;
import cn.javadog.sd.mybatis.builder.xml.ResultMapResolver;
import cn.javadog.sd.mybatis.builder.xml.XMLStatementBuilder;
import cn.javadog.sd.mybatis.executor.BatchExecutor;
import cn.javadog.sd.mybatis.executor.CachingExecutor;
import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.executor.ReuseExecutor;
import cn.javadog.sd.mybatis.executor.SimpleExecutor;
import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.executor.loader.ProxyFactory;
import cn.javadog.sd.mybatis.executor.loader.cglib.CglibProxyFactory;
import cn.javadog.sd.mybatis.executor.loader.javassist.JavassistProxyFactory;
import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.executor.resultset.DefaultResultSetHandler;
import cn.javadog.sd.mybatis.executor.resultset.ResultSetHandler;
import cn.javadog.sd.mybatis.executor.statement.RoutingStatementHandler;
import cn.javadog.sd.mybatis.executor.statement.StatementHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.Environment;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ParameterMap;
import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.plugin.Interceptor;
import cn.javadog.sd.mybatis.plugin.InterceptorChain;
import cn.javadog.sd.mybatis.scripting.LanguageDriver;
import cn.javadog.sd.mybatis.scripting.LanguageDriverRegistry;
import cn.javadog.sd.mybatis.scripting.defaults.RawLanguageDriver;
import cn.javadog.sd.mybatis.scripting.xmltags.XMLLanguageDriver;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.cache.decorators.FifoCache;
import cn.javadog.sd.mybatis.support.cache.decorators.LruCache;
import cn.javadog.sd.mybatis.support.cache.impl.PerpetualCache;
import cn.javadog.sd.mybatis.support.datasource.pooled.PooledDataSourceFactory;
import cn.javadog.sd.mybatis.support.datasource.unpooled.UnpooledDataSourceFactory;
import cn.javadog.sd.mybatis.support.io.VFS;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.logging.nologging.NoLoggingImpl;
import cn.javadog.sd.mybatis.support.logging.slf4j.Slf4jImpl;
import cn.javadog.sd.mybatis.support.logging.stdout.StdOutImpl;
import cn.javadog.sd.mybatis.support.parsing.XNode;
import cn.javadog.sd.mybatis.support.reflection.factory.DefaultObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.DefaultReflectorFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ReflectorFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.wrapper.DefaultObjectWrapperFactory;
import cn.javadog.sd.mybatis.support.reflection.wrapper.ObjectWrapperFactory;
import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.transaction.jdbc.JdbcTransactionFactory;
import cn.javadog.sd.mybatis.support.transaction.managed.ManagedTransactionFactory;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeAliasRegistry;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;
import cn.javadog.sd.mybatis.support.type.handler.EnumTypeHandler;

/**
 * @author 余勇
 * @date 2019-12-17 16:44
 * 全局配置
 */
public class Configuration {

  /**
   * 环境对象
   */
  protected Environment environment;

  /**
   * 允许在嵌套语句中使用分页（RowBounds）。如果允许使用则设置为 false
   */
  protected boolean safeRowBoundsEnabled;

  /**
   * 允许在嵌套语句中使用分页（ResultHandler）。如果允许使用则设置为 false。
   */
  protected boolean safeResultHandlerEnabled = true;

  /**
   * 是否开启自动驼峰命名规则（camel case）映射，即从经典数据库列名 A_COLUMN 到经典 Java 属性名 aColumn 的类似映射。
   */
  protected boolean mapUnderscoreToCamelCase;

  /**
   * 当开启时，任何方法的调用都会加载该对象的所有属性。否则，每个属性会按需加载（参考lazyLoadTriggerMethods)
   * 参考：https://www.jianshu.com/p/57bf52d2dde1
   */
  protected boolean aggressiveLazyLoading;

  /**
   * 是否允许单一语句返回多结果集（需要驱动支持）。
   */
  protected boolean multipleResultSetsEnabled = true;

  /**
   * 允许 JDBC 支持自动生成主键，需要驱动支持。
   * 如果设置为 true 则这个设置强制使用自动生成主键，尽管一些驱动不能支持但仍可正常工作（比如 Derby）。
   */
  protected boolean useGeneratedKeys;

  /**
   * 使用列标签代替列名。也就是别名
   * 不同的驱动在这方面会有不同的表现，具体可参考相关驱动文档或通过测试这两种不同的模式来观察所用驱动的结果。
   */
  protected boolean useColumnLabel = true;

  /**
   * 全局地开启或关闭配置文件中的所有映射器已经配置的任何缓存。
   * 二级缓存
   */
  protected boolean cacheEnabled = true;

  /**
   * 指定当结果集中值为 null 的时候是否调用映射对象的 setter（map 对象时为 put）方法，
   * 这在依赖于 Map.keySet() 或 null 值初始化的时候比较有用。
   * 注意基本类型（int、boolean 等）是不能设置成 null 的。
   */
  protected boolean callSettersOnNulls;

  /**
   * 允许使用方法签名中的名称作为语句参数名称。
   * 为了使用该特性，你的项目必须采用 Java 8 编译，并且加上 -parameters 选项。（新增于 3.4.1）
   */
  protected boolean useActualParamName = true;

  /**
   * 当查询的返回一行都是null的结果时，MyBatis会帮忙填充一个所有属性都是null的对象。
   */
  protected boolean returnInstanceForEmptyRow;

  /**
   * 指定 MyBatis 增加到日志名称的前缀。
   */
  protected String logPrefix;

  /**
   * log实现类
   */
  protected Class <? extends Log> logImpl;

  /**
   * vfs实现类
   * todo 感觉此属性意义不大
   */
  protected Class <? extends VFS> vfsImpl;

  /**
   * 一级缓存的作用域
   */
  protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;

  /**
   * 指定参数为null时，使用的jdbc类型
   */
  protected JdbcType jdbcTypeForNull = JdbcType.OTHER;

  /**
   * 指定哪个对象的方法触发一次延迟加载。
   */
  protected Set<String> lazyLoadTriggerMethods = new HashSet<String>(Arrays.asList("equals", "clone", "hashCode", "toString"));

  /**
   * 默认的Statement超时时间
   */
  protected Integer defaultStatementTimeout;

  /**
   * 默认的FetchSize
   */
  protected Integer defaultFetchSize;

  /**
   * 默认的执行器类型
   */
  protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;

  /**
   * 自定映射行为
   */
  protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;

  /**
   * 未匹配上的列的映射行为
   */
  protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;

  /**
   * 全局变量，可用于替换占位符
   */
  protected Properties variables = new Properties();

  /**
   * 反射工厂
   */
  protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();

  /**
   * 对象工厂
   */
  protected ObjectFactory objectFactory = new DefaultObjectFactory();

  /**
   * 包装工厂
   */
  protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();

  /**
   * 是否开启懒加载
   */
  protected boolean lazyLoadingEnabled = false;

  /**
   * 代理工厂
   * #224 使用内部的Javassist代替OGNL？
   */
  protected ProxyFactory proxyFactory = new JavassistProxyFactory();

  /**
   * 配置工厂类
   * 用于创建配置类，反序列化属性
   * @see <a href='https://code.google.com/p/mybatis/issues/detail?id=300'>Issue 300 (google code)</a>
   */
  protected Class<?> configurationFactory;

  /**
   * mapper注册表
   */
  protected final MapperRegistry mapperRegistry = new MapperRegistry(this);

  /**
   * 拦截链表
   */
  protected final InterceptorChain interceptorChain = new InterceptorChain();

  /**
   * 类型处理器注册表
   */
  protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();

  /**
   * 类型别名注册表
   */
  protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();

  /**
   * 语言驱动注册表
   */
  protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

  /**
   * mappedStatements映射，key是相应的ID
   */
  protected final Map<String, MappedStatement> mappedStatements = new StrictMap<>("Mapped Statements collection");

  /**
   * Cache 对象集合，二级缓存
   *
   * KEY：命名空间 namespace
   */
  protected final Map<String, Cache> caches = new StrictMap<>("Caches collection");

  /**
   * ResultMap 注册表
   */
  protected final Map<String, ResultMap> resultMaps = new StrictMap<>("Result Maps collection");

  /**
   * ParameterMap 注册表
   */
  protected final Map<String, ParameterMap> parameterMaps = new StrictMap<>("Parameter Maps collection");

  /**
   * 主键生成器注册表
   */
  protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<>("Key Generators collection");

  /**
   * 要加载的资源
   */
  protected final Set<String> loadedResources = new HashSet<>();

  /**
   * sql短语集合
   */
  protected final Map<String, XNode> sqlFragments = new StrictMap<>("XML fragments parsed from previous mappers");

  /**
   * 未解析完成的Statement
   */
  protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<>();

  /**
   * 未解析完成的CacheRef
   */
  protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<>();

  /**
   * 未解析完成的ResultMap
   */
  protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<>();

  /**
   * 未解析完成的Method
   */
  protected final Collection<MethodResolver> incompleteMethods = new LinkedList<>();

  /**
   * 存储cache-ref依赖关系的map。
   * key：cache-ref所在的mapper
   * value：cache-ref指向的mapper
   */
  protected final Map<String, String> cacheRefMap = new HashMap<>();

  /**
   * 构造函数
   */
  public Configuration(Environment environment) {
    this();
    this.environment = environment;
  }

  /**
   * 构造函数
   */
  public Configuration() {
    // 注册别名
    // 事务管理器类型
    typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
    typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);
    // 连接池类型
    typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
    typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);
    // 缓存实现类类型
    typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
    typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
    typeAliasRegistry.registerAlias("LRU", LruCache.class);
    // 语言驱动类型
    typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
    typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);
    // 日志打印器类型
    typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
    typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
    typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);
    // 动态代理工厂类型
    typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
    typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
    // 注册到 languageRegistry 中
    languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
    languageRegistry.register(RawLanguageDriver.class);
  }

  /*一堆get/set*/

  public String getLogPrefix() {
    return logPrefix;
  }

  public void setLogPrefix(String logPrefix) {
    this.logPrefix = logPrefix;
  }

  public Class<? extends Log> getLogImpl() {
    return logImpl;
  }

  public void setLogImpl(Class<? extends Log> logImpl) {
    if (logImpl != null) {
      this.logImpl = logImpl;
      LogFactory.useCustomLogging(this.logImpl);
    }
  }

  public Class<? extends VFS> getVfsImpl() {
    return this.vfsImpl;
  }

  public void setVfsImpl(Class<? extends VFS> vfsImpl) {
    if (vfsImpl != null) {
      // 设置 vfsImpl 属性
      this.vfsImpl = vfsImpl;
      // 添加到 VFS 中的自定义 VFS 类的集合
      VFS.addImplClass(this.vfsImpl);
    }
  }

  public boolean isCallSettersOnNulls() {
    return callSettersOnNulls;
  }

  public void setCallSettersOnNulls(boolean callSettersOnNulls) {
    this.callSettersOnNulls = callSettersOnNulls;
  }

  public boolean isUseActualParamName() {
    return useActualParamName;
  }

  public void setUseActualParamName(boolean useActualParamName) {
    this.useActualParamName = useActualParamName;
  }

  public boolean isReturnInstanceForEmptyRow() {
    return returnInstanceForEmptyRow;
  }

  public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance) {
    this.returnInstanceForEmptyRow = returnEmptyInstance;
  }

  public Class<?> getConfigurationFactory() {
    return configurationFactory;
  }

  public void setConfigurationFactory(Class<?> configurationFactory) {
    this.configurationFactory = configurationFactory;
  }

  public boolean isSafeResultHandlerEnabled() {
    return safeResultHandlerEnabled;
  }

  public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
    this.safeResultHandlerEnabled = safeResultHandlerEnabled;
  }

  public boolean isSafeRowBoundsEnabled() {
    return safeRowBoundsEnabled;
  }

  public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
    this.safeRowBoundsEnabled = safeRowBoundsEnabled;
  }

  public boolean isMapUnderscoreToCamelCase() {
    return mapUnderscoreToCamelCase;
  }

  public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
    this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
  }

  public void addLoadedResource(String resource) {
    loadedResources.add(resource);
  }

  public boolean isResourceLoaded(String resource) {
    return loadedResources.contains(resource);
  }

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public AutoMappingBehavior getAutoMappingBehavior() {
    return autoMappingBehavior;
  }

  public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
    this.autoMappingBehavior = autoMappingBehavior;
  }

  /**
   * @since 3.4.0
   */
  public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
    return autoMappingUnknownColumnBehavior;
  }

  /**
   * @since 3.4.0
   */
  public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
    this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
  }

  public boolean isLazyLoadingEnabled() {
    return lazyLoadingEnabled;
  }

  public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
    this.lazyLoadingEnabled = lazyLoadingEnabled;
  }

  public ProxyFactory getProxyFactory() {
    return proxyFactory;
  }

  public void setProxyFactory(ProxyFactory proxyFactory) {
    if (proxyFactory == null) {
      proxyFactory = new JavassistProxyFactory();
    }
    this.proxyFactory = proxyFactory;
  }

  public boolean isAggressiveLazyLoading() {
    return aggressiveLazyLoading;
  }

  public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
    this.aggressiveLazyLoading = aggressiveLazyLoading;
  }

  public boolean isMultipleResultSetsEnabled() {
    return multipleResultSetsEnabled;
  }

  public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
    this.multipleResultSetsEnabled = multipleResultSetsEnabled;
  }

  public Set<String> getLazyLoadTriggerMethods() {
    return lazyLoadTriggerMethods;
  }

  public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
    this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
  }

  public boolean isUseGeneratedKeys() {
    return useGeneratedKeys;
  }

  public void setUseGeneratedKeys(boolean useGeneratedKeys) {
    this.useGeneratedKeys = useGeneratedKeys;
  }

  public ExecutorType getDefaultExecutorType() {
    return defaultExecutorType;
  }

  public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
    this.defaultExecutorType = defaultExecutorType;
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public void setCacheEnabled(boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }

  public Integer getDefaultStatementTimeout() {
    return defaultStatementTimeout;
  }

  public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
    this.defaultStatementTimeout = defaultStatementTimeout;
  }

  /**
   * @since 3.3.0
   */
  public Integer getDefaultFetchSize() {
    return defaultFetchSize;
  }

  /**
   * @since 3.3.0
   */
  public void setDefaultFetchSize(Integer defaultFetchSize) {
    this.defaultFetchSize = defaultFetchSize;
  }

  public boolean isUseColumnLabel() {
    return useColumnLabel;
  }

  public void setUseColumnLabel(boolean useColumnLabel) {
    this.useColumnLabel = useColumnLabel;
  }

  public LocalCacheScope getLocalCacheScope() {
    return localCacheScope;
  }

  public void setLocalCacheScope(LocalCacheScope localCacheScope) {
    this.localCacheScope = localCacheScope;
  }

  public JdbcType getJdbcTypeForNull() {
    return jdbcTypeForNull;
  }

  public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
    this.jdbcTypeForNull = jdbcTypeForNull;
  }

  public Properties getVariables() {
    return variables;
  }

  public void setVariables(Properties variables) {
    this.variables = variables;
  }

  public TypeHandlerRegistry getTypeHandlerRegistry() {
    return typeHandlerRegistry;
  }

  /**
   * Set a default {@link TypeHandler} class for {@link Enum}.
   * A default {@link TypeHandler} is {@link EnumTypeHandler}.
   * @param typeHandler a type handler class for {@link Enum}
   * @since 3.4.5
   */
  public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
    if (typeHandler != null) {
      getTypeHandlerRegistry().setDefaultEnumTypeHandler(typeHandler);
    }
  }

  public TypeAliasRegistry getTypeAliasRegistry() {
    return typeAliasRegistry;
  }

  /**
   * @since 3.2.2
   */
  public MapperRegistry getMapperRegistry() {
    return mapperRegistry;
  }

  public ReflectorFactory getReflectorFactory() {
	  return reflectorFactory;
  }

  public void setReflectorFactory(ReflectorFactory reflectorFactory) {
	  this.reflectorFactory = reflectorFactory;
  }

  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  public void setObjectFactory(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public ObjectWrapperFactory getObjectWrapperFactory() {
    return objectWrapperFactory;
  }

  public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
    this.objectWrapperFactory = objectWrapperFactory;
  }

  /**
   * @since 3.2.2
   */
  public List<Interceptor> getInterceptors() {
    return interceptorChain.getInterceptors();
  }

  public LanguageDriverRegistry getLanguageRegistry() {
    return languageRegistry;
  }

  public void setDefaultScriptingLanguage(Class<? extends LanguageDriver> driver) {
    if (driver == null) {
      driver = XMLLanguageDriver.class;
    }
    getLanguageRegistry().setDefaultDriverClass(driver);
  }

  public LanguageDriver getDefaultScriptingLanguageInstance() {
    return languageRegistry.getDefaultDriver();
  }

  /** @deprecated Use {@link #getDefaultScriptingLanguageInstance()} */
  @Deprecated
  public LanguageDriver getDefaultScriptingLanuageInstance() {
    return getDefaultScriptingLanguageInstance();
  }

  /**
   * 创建指定对象的元对象
   */
  public MetaObject newMetaObject(Object object) {
    return MetaObject.forObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  /**
   * 创建 ParameterHandler
   */
  public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
    // 应用插件
    parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
    return parameterHandler;
  }

  /**
   * 创建 ResultSetHandler
   */
  public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
      ResultHandler resultHandler, BoundSql boundSql) {
    ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
    // 应用插件
    resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
    return resultSetHandler;
  }

  /**
   * 创建statement
   */
  public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    // 创建 RoutingStatementHandler 对象
    StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    // 应用插件
    statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
    return statementHandler;
  }

  /**
   * 创建执行器
   */
  public Executor newExecutor(Transaction transaction) {
    return newExecutor(transaction, defaultExecutorType);
  }

  /**
   * 创建 Executor 对象
   *
   * @param transaction 事务对象
   * @param executorType 执行器类型
   * @return Executor 对象
   */
  public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    // 获得执行器类型。优先级：自定义 > defaultExecutorType > SIMPLE
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    // 创建对应实现的 Executor 对象
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
      executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
      executor = new ReuseExecutor(this, transaction);
    } else {
      executor = new SimpleExecutor(this, transaction);
    }
    // 如果开启缓存，创建 CachingExecutor 对象，进行包装
    if (cacheEnabled) {
      executor = new CachingExecutor(executor);
    }
    // 应用插件
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
  }

  /*主键生成器相关操作*/

  public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
    keyGenerators.put(id, keyGenerator);
  }

  public Collection<String> getKeyGeneratorNames() {
    return keyGenerators.keySet();
  }

  public Collection<KeyGenerator> getKeyGenerators() {
    return keyGenerators.values();
  }

  public KeyGenerator getKeyGenerator(String id) {
    return keyGenerators.get(id);
  }

  public boolean hasKeyGenerator(String id) {
    return keyGenerators.containsKey(id);
  }

  /*缓存相关操作*/

  public void addCache(Cache cache) {
    caches.put(cache.getId(), cache);
  }

  public Collection<String> getCacheNames() {
    return caches.keySet();
  }

  public Collection<Cache> getCaches() {
    return caches.values();
  }

  public Cache getCache(String id) {
    return caches.get(id);
  }

  public boolean hasCache(String id) {
    return caches.containsKey(id);
  }

  /*resultMap相关操作*/

  /**
   * 添加到 Configuration 的 resultMaps
   */
  public void addResultMap(ResultMap rm) {
    // 添加到 resultMaps 中
    resultMaps.put(rm.getId(), rm);
    // 遍历全局的 ResultMap 集合，若其拥有 Discriminator 对象，则判断是否强制标记为有内嵌的 ResultMap
    checkLocallyForDiscriminatedNestedResultMaps(rm);
    // 若传入的 ResultMap 不存在内嵌 ResultMap 并且有 Discriminator ，则判断是否需要强制标记为有内嵌的 ResultMap
    checkGloballyForDiscriminatedNestedResultMaps(rm);
  }

  public Collection<String> getResultMapNames() {
    return resultMaps.keySet();
  }

  public Collection<ResultMap> getResultMaps() {
    return resultMaps.values();
  }

  public ResultMap getResultMap(String id) {
    return resultMaps.get(id);
  }

  public boolean hasResultMap(String id) {
    return resultMaps.containsKey(id);
  }

  /*ParameterMap相关操作*/

  public void addParameterMap(ParameterMap pm) {
    parameterMaps.put(pm.getId(), pm);
  }

  public Collection<String> getParameterMapNames() {
    return parameterMaps.keySet();
  }

  public Collection<ParameterMap> getParameterMaps() {
    return parameterMaps.values();
  }

  public ParameterMap getParameterMap(String id) {
    return parameterMaps.get(id);
  }

  public boolean hasParameterMap(String id) {
    return parameterMaps.containsKey(id);
  }

  /*MappedStatement相关操作*/

  public void addMappedStatement(MappedStatement ms) {
    mappedStatements.put(ms.getId(), ms);
  }

  public Collection<String> getMappedStatementNames() {
    buildAllStatements();
    return mappedStatements.keySet();
  }

  public Collection<MappedStatement> getMappedStatements() {
    buildAllStatements();
    return mappedStatements.values();
  }

  /*incompleteStatements相关操作*/

  public Collection<XMLStatementBuilder> getIncompleteStatements() {
    return incompleteStatements;
  }

  public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
    incompleteStatements.add(incompleteStatement);
  }

  /*incompleteCacheRefs相关操作*/

  public Collection<CacheRefResolver> getIncompleteCacheRefs() {
    return incompleteCacheRefs;
  }

  public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
    incompleteCacheRefs.add(incompleteCacheRef);
  }

  /*incompleteResultMaps相关操作*/

  public Collection<ResultMapResolver> getIncompleteResultMaps() {
    return incompleteResultMaps;
  }

  public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
    incompleteResultMaps.add(resultMapResolver);
  }

  /*incompleteMethods相关操作*/

  public void addIncompleteMethod(MethodResolver builder) {
    incompleteMethods.add(builder);
  }

  public Collection<MethodResolver> getIncompleteMethods() {
    return incompleteMethods;
  }

  /**
   * 获取指定的MappedStatement
   */
  public MappedStatement getMappedStatement(String id) {
    return this.getMappedStatement(id, true);
  }

  /**
   * 获取指定的MappedStatement，并校验完全加载完毕
   */
  public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
      // 校验，保证所有 MappedStatement 已经构造完毕
      if (validateIncompleteStatements) {
          buildAllStatements();
      }
      // 获取 MappedStatement 对象
    return mappedStatements.get(id);
  }

  /**
   * 获取 sqlFragments
   */
  public Map<String, XNode> getSqlFragments() {
    return sqlFragments;
  }

  /**
   * 添加拦截器
   */
  public void addInterceptor(Interceptor interceptor) {
    interceptorChain.addInterceptor(interceptor);
  }

  /**
   * 扫描指定包下，指定子类的mapper
   */
  public void addMappers(String packageName, Class<?> superType) {
    mapperRegistry.addMappers(packageName, superType);
  }

  /**
   * 扫描该包下的所有 Mapper 接口
   */
  public void addMappers(String packageName) {
    // 扫描该包下所有的 Mapper 接口，并添加到 mapperRegistry 中
    mapperRegistry.addMappers(packageName);
  }

  /**
   * 直接添加到 configuration
   */
  public <T> void addMapper(Class<T> type) {
    mapperRegistry.addMapper(type);
  }

  /**
   * 获取指定mapper
   */
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    return mapperRegistry.getMapper(type, sqlSession);
  }

  /**
   * 判断是否有指定mapper
   */
  public boolean hasMapper(Class<?> type) {
    return mapperRegistry.hasMapper(type);
  }

  /**
   * 是否有指定Statement
   */
  public boolean hasStatement(String statementName) {
    return hasStatement(statementName, true);
  }

  /**
   * 是否有指定Statement
   */
  public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
    if (validateIncompleteStatements) {
      buildAllStatements();
    }
    return mappedStatements.containsKey(statementName);
  }

  /**
   * 添加缓存引用
   */
  public void addCacheRef(String namespace, String referencedNamespace) {
    cacheRefMap.put(namespace, referencedNamespace);
  }

  /**
   * 解析之前解析失败的statement节点。更加推荐的方式是在所有mapper都已经解析完毕后，再调用此方法一次。这是fail-fast 的检验机制
   * 参见：https://zhuanlan.zhihu.com/p/37476508
   * TODO 为什么只解析了一个
   */
  protected void buildAllStatements() {
    if (!incompleteResultMaps.isEmpty()) {
      synchronized (incompleteResultMaps) {
        // 保证 incompleteResultMaps 被解析完，出错直接抛出 BuilderException
        incompleteResultMaps.iterator().next().resolve();
      }
    }
    if (!incompleteCacheRefs.isEmpty()) {
      synchronized (incompleteCacheRefs) {
        // 保证 incompleteCacheRefs 被解析完，出错直接抛出 BuilderException
        incompleteCacheRefs.iterator().next().resolveCacheRef();
      }
    }
    if (!incompleteStatements.isEmpty()) {
      synchronized (incompleteStatements) {
          // 保证 incompleteStatements 被解析完，出错直接抛出 BuilderException
        incompleteStatements.iterator().next().parseStatementNode();
      }
    }
    if (!incompleteMethods.isEmpty()) {
      synchronized (incompleteMethods) {
          // 保证 incompleteMethods 被解析完，出错直接抛出 BuilderException
        incompleteMethods.iterator().next().resolve();
      }
    }
  }

  /**
   * 从 statementId 中解析 namespace
   */
  protected String extractNamespace(String statementId) {
    int lastPeriod = statementId.lastIndexOf('.');
    return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
  }

  /**
   * 遍历全局的 ResultMap 集合，若其拥有 Discriminator 对象，则判断是否强制标记为有内嵌的 ResultMap
   * 虽然慢，但只需一次
   */
  protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {
    // 如果传入的 ResultMap 有内嵌的 ResultMap
    if (rm.hasNestedResultMaps()) {
      // 遍历全局的 ResultMap 集合
      for (Map.Entry<String, ResultMap> entry : resultMaps.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof ResultMap) {
          ResultMap entryResultMap = (ResultMap) value;
          // 判断遍历的全局的 entryResultMap 不存在内嵌 ResultMap 并且有 Discriminator
          if (!entryResultMap.hasNestedResultMaps() && entryResultMap.getDiscriminator() != null) {
            // 判断是否 Discriminator 的 ResultMap 集合中，使用了传入的 ResultMap
            // 如果是，则标记为有内嵌的 ResultMap
            Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator().getDiscriminatorMap().values();
            if (discriminatedResultMapNames.contains(rm.getId())) {
              entryResultMap.forceNestedResultMaps();
            }
          }
        }
      }
    }
  }

  /**
   * 若传入的 ResultMap 不存在内嵌 ResultMap 并且有 Discriminator ，
   * 则判断是否需要强制表位有内嵌的 ResultMap。
   * 虽然执行起来很耗时间，但一次执行即可，更受欢迎的做法
   */
  protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {
    // 如果传入的 ResultMap 不存在内嵌 ResultMap 并且有 Discriminator
    if (!rm.hasNestedResultMaps() && rm.getDiscriminator() != null) {
      // 遍历传入的 ResultMap 的 Discriminator 的 ResultMap 集合
      for (Map.Entry<String, String> entry : rm.getDiscriminator().getDiscriminatorMap().entrySet()) {
        String discriminatedResultMapName = entry.getValue();
        if (hasResultMap(discriminatedResultMapName)) {
          // 如果引用的 ResultMap 存在内嵌 ResultMap ，则标记传入的 ResultMap 存在内嵌 ResultMap
          ResultMap discriminatedResultMap = resultMaps.get(discriminatedResultMapName);
          if (discriminatedResultMap.hasNestedResultMaps()) {
            rm.forceNestedResultMaps();
            break;
          }
        }
      }
    }
  }

  /**
   * 内部类。存储集合类型的参数会用到，当前类存一些映射关系也会用到
   */
  protected static class StrictMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -4950446264854982944L;

    /**
     * 集合名
     */
    private final String name;

    /**
     * 构造函数
     */
    public StrictMap(String name, int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor);
      this.name = name;
    }

    /**
     * 构造函数
     */
    public StrictMap(String name, int initialCapacity) {
      super(initialCapacity);
      this.name = name;
    }

    /**
     * 构造函数
     */
    public StrictMap(String name) {
      super();
      this.name = name;
    }

    /**
     * 构造函数
     */
    public StrictMap(String name, Map<String, ? extends V> m) {
      super(m);
      this.name = name;
    }

    /**
     * 添加元素
     */
    @Override
    @SuppressWarnings("unchecked")
    public V put(String key, V value) {
      if (containsKey(key)) {
        throw new IllegalArgumentException(name + " already contains value for " + key);
      }
      if (key.contains(".")) {
        // 获取短名称
        final String shortKey = getShortName(key);
        if (super.get(shortKey) == null) {
          super.put(shortKey, value);
        } else {
          // 有冲突的话使用 Ambiguity
          super.put(shortKey, (V) new Ambiguity(shortKey));
        }
      }
      return super.put(key, value);
    }

    /**
     * 获取指定元素
     */
    @Override
    public V get(Object key) {
      V value = super.get(key);
      if (value == null) {
        throw new IllegalArgumentException(name + " does not contain value for " + key);
      }
      // Ambiguity 不可接受
      if (value instanceof Ambiguity) {
        throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
            + " (try using the full name including the namespace, or rename one of the entries)");
      }
      return value;
    }

    /**
     * 获取值的短名称，如  packagea.packageb.classc，就返回 classc
     */
    private String getShortName(String key) {
      final String[] keyParts = key.split("\\.");
      return keyParts[keyParts.length - 1];
    }

    /**
     * 内部类，用于 StrictMap 指定key如果两次put value的话，将会将值记做Ambiguity，意为模棱两可
     */
    protected static class Ambiguity {

      /**
       * value值
       */
      final private String subject;

      /**
       * 构造
       */
      public Ambiguity(String subject) {
        this.subject = subject;
      }

      /**
       * 获取值
       */
      public String getSubject() {
        return subject;
      }
    }
  }

}
