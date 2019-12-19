package cn.javadog.sd.mybatis.builder.xml;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import cn.javadog.sd.mybatis.builder.BaseBuilder;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.executor.loader.ProxyFactory;
import cn.javadog.sd.mybatis.mapping.Environment;
import cn.javadog.sd.mybatis.plugin.Interceptor;
import cn.javadog.sd.mybatis.session.AutoMappingBehavior;
import cn.javadog.sd.mybatis.session.AutoMappingUnknownColumnBehavior;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.LocalCacheScope;
import cn.javadog.sd.mybatis.support.datasource.DataSourceFactory;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.io.VFS;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.parsing.XNode;
import cn.javadog.sd.mybatis.support.parsing.XPathParser;
import cn.javadog.sd.mybatis.support.reflection.factory.DefaultReflectorFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ReflectorFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaClass;
import cn.javadog.sd.mybatis.support.reflection.wrapper.ObjectWrapperFactory;
import cn.javadog.sd.mybatis.support.transaction.TransactionFactory;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;

/**
 * @author 余勇
 * @date 2019-12-10 22:59
 *
 * XML 配置构建器，主要负责解析 mybatis-config.xml 配置文件
 */
public class XMLConfigBuilder extends BaseBuilder {

  /**
   * 是否已解析
   */
  private boolean parsed;

  /**
   * 基于 Java XPath 解析器
   */
  private final XPathParser parser;

  /**
   * 环境
   */
  private String environment;

  /**
   * ReflectorFactory 对象，默认是{@link DefaultReflectorFactory}, 这个基本不会改变
   */
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  /** n个不同的构造*/

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, props), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, props), environment, props);
  }

  /**
   * 👆几个构造的目的地
   */
  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    // 创建 Configuration 对象
    super(new Configuration());
    // 记录下在做什么，如果出错，将会打印
    ErrorContext.instance().resource("SQL Mapper Configuration");
    // 设置 Configuration 的 variables 属性
    this.configuration.setVariables(props);
    // 标记解析未完成
    this.parsed = false;
    // 设置环境，用于解析时选择
    this.environment = environment;
    // 初始化解析器
    this.parser = parser;
  }

  /**
   * 解析 XML 成 Configuration 对象
   */
  public Configuration parse() {
    // 若已解析，抛出 BuilderException 异常
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    // 标记已解析
    parsed = true;
    // 解析 XML configuration 节点
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  /**
   * 解析 <configuration /> 节点
   */
  private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first
      // 解析 <properties /> 标签，note 必须最先解析，原因见 issue #117
      propertiesElement(root.evalNode("properties"));
      // 解析 <settings /> 标签
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      // 加载自定义 VFS 实现类
      loadCustomVfs(settings);
      // 解析 <typeAliases /> 标签
      typeAliasesElement(root.evalNode("typeAliases"));
      // 解析 <plugins /> 标签
      pluginElement(root.evalNode("plugins"));
      // 解析 <objectFactory /> 标签
      objectFactoryElement(root.evalNode("objectFactory"));
      // 解析 <objectWrapperFactory /> 标签
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      // 解析 <reflectorFactory /> 标签
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      // 赋值 <settings /> 到 Configuration 属
      settingsElement(settings);
      // 解析 <environments /> 标签，note 必须在 objectFactory 和 objectWrapperFactory 解析之后再解析，原因见 #631
      environmentsElement(root.evalNode("environments"));
      // 解析 <databaseIdProvider /> 标签
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      // 解析 <typeHandlers /> 标签
      typeHandlerElement(root.evalNode("typeHandlers"));
      // 解析 <mappers /> 标签，从xml去找接口，而不是@mapperScan+mapper.location这种从接口找xml
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  /**
   * 将 <setting /> 标签解析为 Properties 对象
   */
  private Properties settingsAsProperties(XNode context) {
    // 将子标签，解析成 Properties 对象
    if (context == null) {
      return new Properties();
    }
    Properties props = context.getChildrenAsProperties();
    // 校验每个属性，在 Configuration 中，有相应的 setting 方法，否则抛出 BuilderException 异常
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  /**
   * 加载自定义 VFS 实现类
   */
  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    // 获得 vfsImpl 属性
    String value = props.getProperty("vfsImpl");
    if (value != null) {
      // 使用 ',' 作为分隔符，拆成 VFS 类名的数组
      String[] clazzes = value.split(",");
      // 遍历 VFS 类名的数组
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          // 获得 VFS 类
          @SuppressWarnings("unchecked")
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
          // 设置到 Configuration 中，注意configuration的VFSImpl只有一个，但其父类VFS会记录下所有自定义的实现，拿实例时拿生效的
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  /**
   * 解析 <typeAliases /> 标签，将配置类注册到 typeAliasRegistry 中
   */
  private void typeAliasesElement(XNode parent) {
    if (parent != null) {
      // 遍历子节点
      for (XNode child : parent.getChildren()) {
        // 指定为包的情况下，注册包下的每个类
        if ("package".equals(child.getName())) {
          String typeAliasPackage = child.getStringAttribute("name");
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        }
        // 指定类的情况下，直接注册类和别名
        else {
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            // 获得类是否存在
            Class<?> clazz = Resources.classForName(type);
            // 注册到 typeAliasRegistry 中
            if (alias == null) {
              typeAliasRegistry.registerAlias(clazz);
            } else {
              typeAliasRegistry.registerAlias(alias, clazz);
            }
            // 若类不存在，则抛出 BuilderException 异常
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  /**
   * 解析 <plugins /> 标签，添加到 Configuration#interceptorChain 中
   */
  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      // 遍历 <plugins /> 标签
      for (XNode child : parent.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
        // 创建 Interceptor 对象
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        // 设置属性，note 也就是说插件的属性与Spring结合后，是可以在这里以某个前缀统一设置的
        interceptorInstance.setProperties(properties);
        // 添加到 configuration 中
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  /**
   * 解析<objectFactory /> 节点
   */
  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      // 获得 ObjectFactory 的实现类
      String type = context.getStringAttribute("type");
      // 获得 Properties 属性
      Properties properties = context.getChildrenAsProperties();
      // 创建 ObjectFactory 对象
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
      // 设置 Properties 属性，DefaultObjectFactory 并没有实现该方法，因此如果需要扩展，可以在这里
      factory.setProperties(properties);
      // 设置 Configuration 的 objectFactory 属性
      configuration.setObjectFactory(factory);
    }
  }

  /**
   * 解析 <objectWrapperFactory /> 标签
   */
  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      configuration.setObjectWrapperFactory(factory);
    }
  }

  /**
   * 解析 <reflectorFactory /> 节点
   */
  private void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
       // 获得 ReflectorFactory 的实现类
       String type = context.getStringAttribute("type");
       // 创建 ReflectorFactory 对象
       ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
       // 设置 Configuration 的 reflectorFactory 属性
       configuration.setReflectorFactory(factory);
    }
  }

  /**
   * 解析 <properties /> 节点
   * 1. 解析 <properties /> 标签，成 Properties 对象
   * 2. 解析 resource 或者 url 关联的文件，成 Properties 对象，覆盖第1步的结果
   * 2. 覆盖 configuration 中的 Properties 对象到上面的结果
   * 3. 设置结果到 parser 和 configuration 中
   */
  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      // 读取子标签们，为 Properties 对象
      Properties defaults = context.getChildrenAsProperties();
      // 读取 resource 和 url 属性
      String resource = context.getStringAttribute("resource");
      String url = context.getStringAttribute("url");
      // resource 和 url 都存在的情况下，抛出 BuilderException 异常
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      if (resource != null) {
        // 读取本地 Properties 配置文件到 defaults 中。
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        // 读取远程 Properties 配置文件到 defaults 中。
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      // 覆盖 configuration 中的 Properties 对象到 defaults 中
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
      // 设置 defaults 到 parser 和 configuration 中。
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }

  /**
   * 赋值 <settings /> 到 Configuration 属性
   */
  private void settingsElement(Properties props) throws Exception {
    // 设置AutoMappingBehavior，默认是PARTIAL，也就是部分映射
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    // 设置autoMappingUnknownColumnBehavior，默认是NONE，也就是当查询的结果与POJO的字段不能完全一致对应时，如何处理
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    // 设置cacheEnabled，默认true，即默认开启一级缓存
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    // 设置proxyFactory，也就是动态代理，默认是Javassist
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
    // 是否开启懒加载，默认不开启
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    // 是否开启侵入式懒加载，默认不开启
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    // 允许或禁止从单一的语句返回多个结果集(需要驱动程序兼容)，默认开启
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
    // 使用列标签代替列名。这个配置项默认为true，如果改为false，则通过别名的方式无法映射，就是所谓的 select name as nameAlias 这种
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    // 允许 JDBC 支持自动生成主键，需要驱动支持。 如果设置为 true 则这个设置强制使用自动生成主键，尽管一些驱动不能支持但仍可正常工作（比如 Derby）。默认false
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    // 配置默认的执行器。SIMPLE 就是普通的执行器；REUSE 执行器会重用预处理语句（prepared statements）； BATCH 执行器将重用语句并执行批量更新。
    // 注意这里只有三个选项，缓存类型的执行器是在运行时选择的
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    // 设置超时时间，它决定驱动等待数据库响应的秒数。
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    // 为驱动的结果集获取数量（fetchSize）设置一个提示值。此参数只可以在查询设置中被覆盖。
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    // 是否开启自动驼峰命名规则（camel case）映射，即从经典数据库列名 A_COLUMN 到经典 Java 属性名 aColumn 的类似映射。默认不开启
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    // 允许在嵌套语句中使用分页（RowBounds）。如果允许使用则设置为 false(允许代表不安全)。所谓嵌套就是select嵌套select，默认不允许
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    // MyBatis 利用本地缓存机制（Local Cache）防止循环引用（circular references）和加速重复嵌套查询。
    // 默认值为 SESSION，这种情况下会缓存一个会话中执行的所有查询。
    // 若设置值为 STATEMENT，本地会话仅用在语句执行上，对相同 SqlSession 的不同调用将不会共享数据。
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    // 当没有为参数提供特定的 JDBC 类型时，为空值指定 JDBC 类型。 某些驱动需要指定列的 JDBC 类型，多数情况直接用一般类型即可，比如 NULL、VARCHAR 或 OTHER。
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    // 指定哪个对象的方法触发一次延迟加载。TODO 意思是，即使没有去拿关联对象的属性，但调用了主对象的这些方法，就会触发延迟加载？
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    // 允许在嵌套语句中使用分页（ResultHandler）。如果允许使用则设置为 false。默认不允许
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    // 指定动态 SQL 生成的默认语言。如 org.apache.ibatis.scripting.xmltags.XMLLanguageDriver, set逻辑会在为空时设置XMLLanguageDriver
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    // 设置默认的枚举类型处理器，没有设置的话，默认是EnumTypeHandler
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler> typeHandler = (Class<? extends TypeHandler>)resolveClass(props.getProperty("defaultEnumTypeHandler"));
    configuration.setDefaultEnumTypeHandler(typeHandler);
    // 当查询的返回一行都是null的结果时，MyBatis是否填充一个所有属性都是null的对象。默认为不填充
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    // 设置是否允许使用方法签名中的名称作为语句参数名称。用于解析SQL请求时的参数占位符，默认为true
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    // 当返回行的所有列都是空时，MyBatis默认返回 null。 当开启这个设置时，MyBatis会返回一个空实例。 请注意，它也适用于嵌套的结果集 （如集合或关联），默认为 false
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    // 指定 MyBatis 增加到日志名称的前缀，不设置就是空字符串
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    // 指定 MyBatis 所用日志的具体实现，未指定时将自动查找。note 交给框架自动查找就好
    @SuppressWarnings("unchecked")
    Class<? extends Log> logImpl = (Class<? extends Log>)resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
    // 指定一个提供 Configuration 实例的类。通俗点讲，就是工厂。
    // 这个被返回的 Configuration 实例用来加载被反序列化对象的延迟加载属性值。 TODO 反序列化？延迟加载？
    // 这个类必须包含一个签名为static Configuration getConfiguration() 的方法
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  /**
   * 解析 <environments /> 标签
   */
  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {
      // 如果初始化 XMLConfigBuilder 时未指定 environment，就使用 <environments /> 标签里的 'default' 属性对应的环境
      if (environment == null) {
        environment = context.getStringAttribute("default");
      }
      // 遍历 XNode 节点
      for (XNode child : context.getChildren()) {
        // 判断 environment 是否匹配
        String id = child.getStringAttribute("id");
        if (isSpecifiedEnvironment(id)) {
          // 解析 `<transactionManager />` 标签，返回 TransactionFactory 对象
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          // 解析 `<dataSource />` 标签，返回 DataSourceFactory 对象
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          DataSource dataSource = dsFactory.getDataSource();
          // 创建 Environment.Builder 对象
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);
          // 构造 Environment 对象，并设置到 configuration 中
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  /**
   * 解析 <databaseIdProvider />
   *
   * MyBatis 可以根据不同的数据库厂商执行不同的语句，这种多厂商的支持是基于映射语句中的 databaseId 属性。
   * MyBatis 会加载不带 databaseId 属性和带有匹配当前数据库 databaseId 属性的所有语句。
   * 如果同时找到带有 databaseId 和不带 databaseId 的相同语句，则后者会被舍弃。
   * 为支持多厂商特性只要像下面这样在 mybatis-config.xml 文件中加入 databaseIdProvider 即可：
   * note 很少使用到
   */
  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      // 获得 DatabaseIdProvider 的类
      String type = context.getStringAttribute("type");
      // 为了兼容性而加的糟糕补丁
      if ("VENDOR".equals(type)) {
          type = "DB_VENDOR";
      }
      // 获得 Properties 对象
      Properties properties = context.getChildrenAsProperties();
      // 创建 DatabaseIdProvider 对象，并设置对应的属性
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      // 获得对应的 databaseId 编号
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      // 设置到 configuration 中
      configuration.setDatabaseId(databaseId);
    }
  }

  /**
   * 解析 <transactionManager /> 标签，返回 TransactionFactory 对象
   */
  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      // 获得 TransactionFactory 的类
      String type = context.getStringAttribute("type");
      // 获得 Properties 属性
      Properties props = context.getChildrenAsProperties();
      // 创建 TransactionFactory 对象，并设置属性
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  /**
   * 解析 <dataSource /> 标签
   */
  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      // 获得 DataSourceFactory 的类
      String type = context.getStringAttribute("type");
      // 获得 Properties 属性
      Properties props = context.getChildrenAsProperties();
      // 创建 DataSourceFactory 对象，并设置属性
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  /**
   * 解析 <typeHandlers /> 标签
   */
  private void typeHandlerElement(XNode parent) throws Exception {
    if (parent != null) {
      // 遍历子节点
      for (XNode child : parent.getChildren()) {
        // 如果是 package 标签，则扫描该包
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          typeHandlerRegistry.register(typeHandlerPackage);
        }
        // 如果是 typeHandler 标签，则注册该 typeHandler 信息
        else {
          // 获得 javaType、jdbcType、handler
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          // 获得 javaType、jdbcType、handler对应的类
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          // 注册 typeHandler
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  /**
   * 解析 <mappers /> 标签
   */
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      // 遍历子节点
      for (XNode child : parent.getChildren()) {
        // 如果是 package 标签，则扫描该包
        if ("package".equals(child.getName())) {
          // 获得包名
          String mapperPackage = child.getStringAttribute("name");
          // 添加到 configuration 中，注册的逻辑在 configuration 里面
          configuration.addMappers(mapperPackage);
        }
        // 如果是 mapper 标签，
        else {
          // 获得 resource、url、class 属性，优先级为 resource => url => class
          // 其中resource和url是从xml发起的，交给XMLMapperBuilder完成，class是mapper接口发起的，交给configuration内部完成
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          // 使用相对于类路径的资源引用
          if (resource != null && url == null && mapperClass == null) {
            // 异常上下文记录一笔
            ErrorContext.instance().resource(resource);
            // 获得 resource 的 InputStream 对象
            InputStream inputStream = Resources.getResourceAsStream(resource);
            // 创建 XMLMapperBuilder 对象
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            // 执行解析
            mapperParser.parse();
          }
          // 使用完全限定资源定位符（URL）
          else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            // 异常上下文记录一笔
            // 获得 url 的 InputStream 对象
            InputStream inputStream = Resources.getUrlAsStream(url);
            // 创建 XMLMapperBuilder 对象
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            // 执行解析
            mapperParser.parse();
          }
          // 使用映射器接口实现类的完全限定类名，也就是mapper接口
          else if (resource == null && url == null && mapperClass != null) {
            // 获得 Mapper 接口
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            // 添加到 configuration 中，注册逻辑在 configuration 里面
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

  /**
   * 将要被解析的environment标签的ID，是否就是我们需要的
   * note 这里要求两者都不能为空，必须有所指定，因此也不能直接一行代码 environment.equals(id)
   */
  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    } else if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {
      return true;
    }
    return false;
  }

}
