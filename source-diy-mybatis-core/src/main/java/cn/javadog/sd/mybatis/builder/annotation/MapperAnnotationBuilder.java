package cn.javadog.sd.mybatis.builder.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import cn.javadog.sd.mybatis.annotations.Arg;
import cn.javadog.sd.mybatis.annotations.CacheNamespace;
import cn.javadog.sd.mybatis.annotations.CacheNamespaceRef;
import cn.javadog.sd.mybatis.annotations.Case;
import cn.javadog.sd.mybatis.annotations.ConstructorArgs;
import cn.javadog.sd.mybatis.annotations.Delete;
import cn.javadog.sd.mybatis.annotations.DeleteProvider;
import cn.javadog.sd.mybatis.annotations.Insert;
import cn.javadog.sd.mybatis.annotations.InsertProvider;
import cn.javadog.sd.mybatis.annotations.Lang;
import cn.javadog.sd.mybatis.annotations.MapKey;
import cn.javadog.sd.mybatis.annotations.Options;
import cn.javadog.sd.mybatis.annotations.Options.FlushCachePolicy;
import cn.javadog.sd.mybatis.annotations.Property;
import cn.javadog.sd.mybatis.annotations.Result;
import cn.javadog.sd.mybatis.annotations.ResultMap;
import cn.javadog.sd.mybatis.annotations.ResultType;
import cn.javadog.sd.mybatis.annotations.Results;
import cn.javadog.sd.mybatis.annotations.Select;
import cn.javadog.sd.mybatis.annotations.SelectKey;
import cn.javadog.sd.mybatis.annotations.SelectProvider;
import cn.javadog.sd.mybatis.annotations.TypeDiscriminator;
import cn.javadog.sd.mybatis.annotations.Update;
import cn.javadog.sd.mybatis.annotations.UpdateProvider;
import cn.javadog.sd.mybatis.builder.MapperBuilderAssistant;
import cn.javadog.sd.mybatis.builder.xml.XMLMapperBuilder;
import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.keygen.Jdbc3KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.NoKeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.SelectKeyGenerator;
import cn.javadog.sd.mybatis.mapping.Discriminator;
import cn.javadog.sd.mybatis.mapping.FetchType;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ResultFlag;
import cn.javadog.sd.mybatis.mapping.ResultMapping;
import cn.javadog.sd.mybatis.mapping.ResultSetType;
import cn.javadog.sd.mybatis.mapping.SqlCommandType;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.mapping.StatementType;
import cn.javadog.sd.mybatis.scripting.LanguageDriver;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.exceptions.BindingException;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.exceptions.IncompleteElementException;
import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.parsing.PropertyParser;
import cn.javadog.sd.mybatis.support.reflection.resolver.TypeParameterResolver;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.ParamMap;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.UnknownTypeHandler;

/**
 * @author 余勇
 * @date 2019-12-12 22:37
 *
 * Mapper 注解构造器，负责解析 Mapper 接口上的注解
 *
 * note 这里涉及的标示拼接，如resultMapId，statementId, 不会加上namespace，命名空间的添加都是在 assistant 完成的
 */
public class MapperAnnotationBuilder {

  /**
   * SQL 操作注解集合
   */
  private static final Set<Class<? extends Annotation>> SQL_ANNOTATION_TYPES = new HashSet<>();

  /**
   * SQL 操作提供者注解集合
   */
  private static final Set<Class<? extends Annotation>> SQL_PROVIDER_ANNOTATION_TYPES = new HashSet<>();

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * 构建助手
   */
  private final MapperBuilderAssistant assistant;

  /**
   * Mapper 接口类
   */
  private final Class<?> type;

  /**
   * 类加载的时候就初始化上面的值。
   */
  static {
    SQL_ANNOTATION_TYPES.add(Select.class);
    SQL_ANNOTATION_TYPES.add(Insert.class);
    SQL_ANNOTATION_TYPES.add(Update.class);
    SQL_ANNOTATION_TYPES.add(Delete.class);

    SQL_PROVIDER_ANNOTATION_TYPES.add(SelectProvider.class);
    SQL_PROVIDER_ANNOTATION_TYPES.add(InsertProvider.class);
    SQL_PROVIDER_ANNOTATION_TYPES.add(UpdateProvider.class);
    SQL_PROVIDER_ANNOTATION_TYPES.add(DeleteProvider.class);
  }

  /**
   * 构造函数
   */
  public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
    // 将全类名改成路径的形式，note 我们debug的时候应该经常看到(best guess)
    String resource = type.getName().replace('.', '/') + ".java (best guess)";
    // 创建 MapperBuilderAssistant 对象
    this.assistant = new MapperBuilderAssistant(configuration, resource);
    this.configuration = configuration;
    this.type = type;
  }

  /**
   * 解析注解
   */
  public void parse() {
    // 判断当前 Mapper 接口是否应加载过。
    String resource = type.toString();
    if (!configuration.isResourceLoaded(resource)) {
      // 加载对应的 XML Mapper
      loadXmlResource();
      // 标记该 Mapper 接口已经加载过，note 这是为了避免接口重复加载
      configuration.addLoadedResource(resource);
      // 设置 namespace 属性
      assistant.setCurrentNamespace(type.getName());
      // 解析 @CacheNamespace 注解
      parseCache();
      // 解析 @CacheNamespaceRef 注解
      parseCacheRef();
      // 遍历每个方法，解析其上的注解
      Method[] methods = type.getMethods();
      for (Method method : methods) {
        try {
          // 不处理桥接的方法
          if (!method.isBridge()) {
            // 执行解析
            parseStatement(method);
          }
        } catch (IncompleteElementException e) {
          // 解析失败，添加到 configuration 中
          configuration.addIncompleteMethod(new MethodResolver(this, method));
        }
      }
    }
    // 解析待定的方法，也就是之前解析失败的方法
    parsePendingMethods();
  }

  /**
   * 解析待定的方法，也就是之前解析失败的方法
   */
  private void parsePendingMethods() {
    // 获得 MethodResolver 集合，并遍历进行处理
    Collection<MethodResolver> incompleteMethods = configuration.getIncompleteMethods();
    synchronized (incompleteMethods) {
      Iterator<MethodResolver> iter = incompleteMethods.iterator();
      while (iter.hasNext()) {
        try {
          // 执行解析
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // 依然报错，不做处理，也不会移除 incompleteMethods。TODO 是会在启动完毕检查，还是不管
        }
      }
    }
  }

  /**
   * 加载对应的 XML Mapper
   */
  private void loadXmlResource() {
    // 判断 Mapper XML 是否已经加载过，如果加载过，就不加载了。
    // 此处，是为了避免和 XMLMapperBuilder#parse() 方法冲突，重复解析
    if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
      // 获得 InputStream 对象，note 这里可以看出名字必须一致
      String xmlResource = type.getName().replace('.', '/') + ".xml";
      InputStream inputStream = null;
      try {
        // 从classpath下拿的，TODO 印象里是从当前路径下拿的，没有给别的口子，忘了哪里看到的
        inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
      } catch (IOException e) {
        // 资源没找到就不管，并不去报错
      }
      // 创建 XMLMapperBuilder 对象，执行解析
      if (inputStream != null) {
        // 也就是说，实际也是交给 XMLMapperBuilder 去解析
        XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource, configuration.getSqlFragments(), type.getName());
        xmlParser.parse();
      }
    }
  }

  /**
   * 解析 @CacheNamespace 注解
   */
  private void parseCache() {
    // 获得类上的 @CacheNamespace 注解
    CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
    if (cacheDomain != null) {
      // 获得 size 属性，这只在某些自定义的缓存装饰类，其需要size属性才会用到
      Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
      // 注意，为0时，取null，即代表不会去包装定时清空的缓存装饰器，也就是不回去定时清空
      Long flushInterval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
      // 获得 Properties 属性
      Properties props = convertToProperties(cacheDomain.properties());
      // 创建 Cache 对象，可以看到其他属性的获取直接在方法调用上面，没有像上面单独拿出来一行，因为上面两个参数涉及到默认值的处理
      assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInterval, size, cacheDomain.readWrite(), cacheDomain.blocking(), props);
    }
  }

  /**
   * 将 @Property 注解数组，转换成 Properties 对象
   */
  private Properties convertToProperties(Property[] properties) {
    if (properties.length == 0) {
      return null;
    }
    Properties props = new Properties();
    for (Property property : properties) {
      props.setProperty(property.name(),
          // value是允许包含占位符的，因此先解析一下
          PropertyParser.parse(property.value(), configuration.getVariables()));
    }
    return props;
  }

  /**
   * 解析 @CacheNamespaceRef 注解
   */
  private void parseCacheRef() {
    // 获得类上的 @CacheNamespaceRef 注解
    CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
    if (cacheDomainRef != null) {
      // 命名空间类，也就是mapper接口，而不是缓存类
      Class<?> refType = cacheDomainRef.value();
      // 命名空间
      String refName = cacheDomainRef.name();
      // 校验，如果 refType 和 refName 都为空，则抛出 BuilderException 异常
      if (refType == void.class && refName.isEmpty()) {
        throw new BuilderException("Should be specified either value() or name() attribute in the @CacheNamespaceRef");
      }
      // 校验，如果 refType 和 refName 都不为空，则抛出 BuilderException 异常，呵呵
      if (refType != void.class && !refName.isEmpty()) {
        throw new BuilderException("Cannot use both value() and name() attribute in the @CacheNamespaceRef");
      }
      // 获得最终的 namespace 属性。TODO 命名空间不应该是全类名吗
      String namespace = (refType != void.class) ? refType.getName() : refName;
      // 获得指向的 Cache 对象
      assistant.useCacheRef(namespace);
    }
  }

  /**
   * 解析其它注解，返回 resultMapId 属性
   */
  private String parseResultMap(Method method) {
    // 获得返回类型
    Class<?> returnType = getReturnType(method);
    // 获得 @ConstructorArgs、@Results、@TypeDiscriminator 注解
    ConstructorArgs args = method.getAnnotation(ConstructorArgs.class);
    Results results = method.getAnnotation(Results.class);
    TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
    // 生成 resultMapId
    String resultMapId = generateResultMapName(method);
    // 生成 ResultMap 对象
    applyResultMap(resultMapId, returnType, argsIf(args), resultsIf(results), typeDiscriminator);
    return resultMapId;
  }

  /**
   * 生成 resultMapId
   */
  private String generateResultMapName(Method method) {
    // 第一种情况，已经声明
    // 如果有 @Results 注解，并且有设置 id 属性，则直接返回。格式为：`${type.name}.${Results.id}` 。
    // note 这里没有拼接命名空间，命名空间的拼接在 builderAssistant 里面完成
    Results results = method.getAnnotation(Results.class);
    if (results != null && !results.id().isEmpty()) {
      return type.getName() + "." + results.id();
    }
    // 第二种情况，自动生成
    // 获得 suffix 前缀，相当于方法参数构成的签名
    StringBuilder suffix = new StringBuilder();
    for (Class<?> c : method.getParameterTypes()) {
      suffix.append("-");
      suffix.append(c.getSimpleName());
    }
    if (suffix.length() < 1) {
      suffix.append("-void");
    }
    // 拼接返回。格式为 `${type.name}.${method.name}${suffix}` 。
    return type.getName() + "." + method.getName() + suffix;
  }

  /**
   * 创建 ResultMap 对象
   */
  private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results, TypeDiscriminator discriminator) {
    // 创建 ResultMapping 数组
    List<ResultMapping> resultMappings = new ArrayList<>();
    // 将 @Arg[] 注解数组，解析成对应的 ResultMapping 对象们，并添加到 resultMappings 中。
    applyConstructorArgs(args, returnType, resultMappings);
    // 将 @Result[] 注解数组，解析成对应的 ResultMapping 对象们，并添加到 resultMappings 中。
    applyResults(results, returnType, resultMappings);
    // 创建 Discriminator 对象
    Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
    // TODO add AutoMappingBehaviour。额，这是源码里面的
    // ResultMap 对象
    assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
    // 创建 Discriminator 的 ResultMap 对象们，note，很奇怪，这里并没有用到上面创建的 Discriminator 对象，似乎上面创建的只是为了上面一级的ResultMap用的
    createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
  }

  /**
   * 创建 Discriminator 的 ResultMap 对象们
   */
  private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
    if (discriminator != null) {
      // 遍历 @Case 注解
      for (Case c : discriminator.cases()) {
        // 创建 @Case 注解的 ResultMap 的编号
        String caseResultMapId = resultMapId + "-" + c.value();
        // 创建 ResultMapping 数组
        List<ResultMapping> resultMappings = new ArrayList<>();
        // issue #136
        // 将 @Arg[] 注解数组，解析成对应的 ResultMapping 对象们，并添加到 resultMappings 中。
        applyConstructorArgs(c.constructArgs(), resultType, resultMappings);
        // 将 @Result[] 注解数组，解析成对应的 ResultMapping 对象们，并添加到 resultMappings 中。
        applyResults(c.results(), resultType, resultMappings);
        // TODO add AutoMappingBehaviour。额，这是源码里面的
        // 创建 ResultMap 对象，每一个 case 都是一个 ResultMap
        assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
      }
    }
  }

  /**
   * 创建 Discriminator 对象
   */
  private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
    if (discriminator != null) {
      // 解析各种属性
      String column = discriminator.column();
      Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
      JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
      @SuppressWarnings("unchecked")
      // 获得 TypeHandler 类
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              (discriminator.typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
      // 遍历 @Case[] 注解数组，解析成 discriminatorMap 集合
      Case[] cases = discriminator.cases();
      Map<String, String> discriminatorMap = new HashMap<>();
      for (Case c : cases) {
        String value = c.value();
        // 拼接 resultMapId
        String caseResultMapId = resultMapId + "-" + value;
        discriminatorMap.put(value, caseResultMapId);
      }
      // 创建 Discriminator 对象
      return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
    }
    return null;
  }

  /**
   * 解析方法上的 SQL 操作相关的注解，最终构建成 MappedStatement 对象
   */
  void parseStatement(Method method) {
    // 获得参数的类型
    Class<?> parameterTypeClass = getParameterType(method);
    // 获得 LanguageDriver 对象
    LanguageDriver languageDriver = getLanguageDriver(method);
    // 获得 SqlSource 对象
    SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);
    if (sqlSource != null) {
      // 获得各种属性
      Options options = method.getAnnotation(Options.class);
      // 拼接 mappedStatementId
      final String mappedStatementId = type.getName() + "." + method.getName();
      // 设置一些默认值
      Integer fetchSize = null;
      Integer timeout = null;
      StatementType statementType = StatementType.PREPARED;
      ResultSetType resultSetType = null;
      SqlCommandType sqlCommandType = getSqlCommandType(method);
      boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
      // 查询语句就不刷新缓存，通俗点说，更新语句就会删除缓存
      boolean flushCache = !isSelect;
      // 查询语句就使用缓存
      boolean useCache = isSelect;

      // 获得 KeyGenerator 对象
      KeyGenerator keyGenerator;
      String keyProperty = null;
      String keyColumn = null;
      // 处理新增更新，因为 SelectKey 不一定只是处理主键，还有可能将更新时间反写
      if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
        // 获取 @SelectKey 注解
        SelectKey selectKey = method.getAnnotation(SelectKey.class);
        if (selectKey != null) {
          // 处理@SelectKey 注解优先级最高
          keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method), languageDriver);
          keyProperty = selectKey.keyProperty();
        } else if (options == null) {
          // 如果无 @Options 注解，则根据全局配置处理
          keyGenerator = configuration.isUseGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
        } else {
          // 如果有 @Options 注解，则使用该注解的配置处理
          keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
          keyProperty = options.keyProperty();
          keyColumn = options.keyColumn();
        }
      } else {
        // DELETE的清空不用主键
        keyGenerator = NoKeyGenerator.INSTANCE;
      }

      // 初始化各种属性，覆盖默认值
      if (options != null) {
        if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
          flushCache = true;
        } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
          flushCache = false;
        }
        useCache = options.useCache();
        // issue #348 TODO 看起来很累赘
        fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null;
        timeout = options.timeout() > -1 ? options.timeout() : null;
        statementType = options.statementType();
        resultSetType = options.resultSetType();
      }

      // 获得 resultMapId 编号字符串
      String resultMapId = null;
      // 如果有 @ResultMap 注解，使用该注解为 resultMapId 属性
      ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
      if (resultMapAnnotation != null) {
        // 这也可能是数组
        String[] resultMaps = resultMapAnnotation.value();
        StringBuilder sb = new StringBuilder();
        for (String resultMap : resultMaps) {
          if (sb.length() > 0) {
            sb.append(",");
          }
          sb.append(resultMap);
        }
        resultMapId = sb.toString();
        // 如果无 @ResultMap 注解，且是查询语句，那解析其它注解，作为 resultMapId 属性
      } else if (isSelect) {
        resultMapId = parseResultMap(method);
      }

      // 构建 MappedStatement 对象
      assistant.addMappedStatement(
          mappedStatementId,
          sqlSource,
          statementType,
          sqlCommandType,
          fetchSize,
          timeout,
          // ParameterMapID
          null,
          parameterTypeClass,
          resultMapId,
          // 获得返回类型
          getReturnType(method),
          resultSetType,
          flushCache,
          useCache,
          // TODO gcode issue #577
          false,
          keyGenerator,
          keyProperty,
          keyColumn,
          // DatabaseID
          null,
          languageDriver,
          // ResultSets
          options != null ? nullOrEmpty(options.resultSets()) : null);
    }
  }

  /**
   * 获得 LanguageDriver 对象
   */
  private LanguageDriver getLanguageDriver(Method method) {
    // 解析 @Lang 注解，获得对应的类型
    Lang lang = method.getAnnotation(Lang.class);
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = lang.value();
    }
    // 如果 langClass 为空，即无 @Lang 注解，则会使用默认 LanguageDriver 类型
    return assistant.getLanguageDriver(langClass);
  }

  /**
   * 获得参数的类型
   */
  private Class<?> getParameterType(Method method) {
    Class<?> parameterType = null;
    Class<?>[] parameterTypes = method.getParameterTypes();
    // 遍历参数类型数组
    for (Class<?> currentParameterType : parameterTypes) {
      //// 排除 RowBounds 和 ResultHandler 两种参数
      if (!RowBounds.class.isAssignableFrom(currentParameterType) && !ResultHandler.class.isAssignableFrom(currentParameterType)) {
        if (parameterType == null) {
          // 如果是多参数，则是 ParamMap 类型
          parameterType = currentParameterType;
        } else {
          // 如果是单参数，则是该参数的类型
          parameterType = ParamMap.class;
        }
      }
    }
    return parameterType;
  }

  /**
   * 获得返回类型
   */
  private Class<?> getReturnType(Method method) {
    // 获得方法的返回类型
    Class<?> returnType = method.getReturnType();
    // 解析成对应的 Type
    Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
    // 如果 Type 是 Class ，普通类
    if (resolvedReturnType instanceof Class) {
      returnType = (Class<?>) resolvedReturnType;
      // 如果是数组类型，则使用 componentType
      if (returnType.isArray()) {
        returnType = returnType.getComponentType();
      }
      // 如果返回类型是 void ，则尝试使用 @ResultType 注解，可以看看 issue #508
      if (void.class.equals(returnType)) {
        ResultType rt = method.getAnnotation(ResultType.class);
        if (rt != null) {
          returnType = rt.value();
        }
      }
    }
    // 如果 Type 是 ParameterizedType ，泛型
    else if (resolvedReturnType instanceof ParameterizedType) {
      // 获得泛型 rawType
      ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      // 如果rawType是 Collection 或者 Cursor 类型时
      if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
        // 获得 <> 中实际类型
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        // 如果 actualTypeArguments 的大小为 1 ，进一步处理
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          Type returnTypeParameter = actualTypeArguments[0];
          // 如果是 Class ，则直接使用 Class
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          }
          // 如果是 ParameterizedType ，则获取 <> 中实际类型
          else if (returnTypeParameter instanceof ParameterizedType) {
            // 这个类型依然有可能是 ParameterizedType，可以看看 issue #443
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          }
          // 如果是泛型数组类型，则获得 genericComponentType 对应的类
          else if (returnTypeParameter instanceof GenericArrayType) {
            Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
            // 支持 List<byte[]> ，可以看看issue #525
            returnType = Array.newInstance(componentType, 0).getClass();
          }
        }
      }
      // 如果有 @MapKey 注解，并且是 Map 类型，如果没有MapKey 注解的话，即使返回类型是map也不管，因为加了这个注解，map的value才是POJO类，否则只是某个字段的值。
      // 可以看看 issue 504
      else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
        // 获得 <> 中实际类型
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
          // 如果 actualTypeArguments 的大小为 2 ，进一步处理。为什么是 2 ，因为 Map<K, V> 呀，有 K、V 两个泛型
          if (actualTypeArguments != null && actualTypeArguments.length == 2) {
            // 处理 V 泛型
            Type returnTypeParameter = actualTypeArguments[1];
            // 如果 V 泛型为 Class ，则直接使用 Class
            if (returnTypeParameter instanceof Class<?>) {
              returnType = (Class<?>) returnTypeParameter;
            }
            // 如果 V 泛型为 ParameterizedType ，则获取 <> 中实际类型
            else if (returnTypeParameter instanceof ParameterizedType) {
              // 这里拿到的rawType依然可能是个ParameterizedType，可以看看issue 443
              returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
            }
          }
      }
      // 如果是 Optional 类型时
      else if (Optional.class.equals(rawType)) {
        // 获得 <> 中实际类型
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        // 因为是 Optional<T> 类型，所以 actualTypeArguments 数组大小是一
        Type returnTypeParameter = actualTypeArguments[0];
        // 如果 <T> 泛型为 Class ，则直接使用 Class
        if (returnTypeParameter instanceof Class<?>) {
          returnType = (Class<?>) returnTypeParameter;
        }
      }
    }

    return returnType;
  }

  /**
   * 从注解中，获得 SqlSource 对象
   */
  private SqlSource getSqlSourceFromAnnotations(Method method, Class<?> parameterType, LanguageDriver languageDriver) {
    try {
      // 获得方法上的 SQL_ANNOTATION_TYPES 对应的类型
      Class<? extends Annotation> sqlAnnotationType = getSqlAnnotationType(method);
      // 获得方法上的 SQL_PROVIDER_ANNOTATION_TYPES 对应的类型，优先级低于 sqlAnnotationType
      Class<? extends Annotation> sqlProviderAnnotationType = getSqlProviderAnnotationType(method);
      // 如果 SQL_ANNOTATION_TYPES 对应的类型非空
      if (sqlAnnotationType != null) {
        // 如果 SQL_PROVIDER_ANNOTATION_TYPES 对应的类型非空，则抛出 BindingException 异常，因为冲突了，不允许加两种类型的注解。
        if (sqlProviderAnnotationType != null) {
          throw new BindingException("You cannot supply both a static SQL and SqlProvider to method named " + method.getName());
        }
        // 获得 SQL_ANNOTATION_TYPES 对应的注解
        Annotation sqlAnnotation = method.getAnnotation(sqlAnnotationType);
        // 获得 value 属性，也就是SQL语句，note 是数组，也就是说支持多条sql
        final String[] strings = (String[]) sqlAnnotation.getClass().getMethod("value").invoke(sqlAnnotation);
        // 创建 SqlSource 对象
        return buildSqlSourceFromStrings(strings, parameterType, languageDriver);
      // <3> 如果 SQL_PROVIDER_ANNOTATION_TYPES 对应的类型非空
      } else if (sqlProviderAnnotationType != null) {
        // 获得 SQL_PROVIDER_ANNOTATION_TYPES 对应的注解
        Annotation sqlProviderAnnotation = method.getAnnotation(sqlProviderAnnotationType);
        // 创建 ProviderSqlSource 对象
        return new ProviderSqlSource(assistant.getConfiguration(), sqlProviderAnnotation, type, method);
      }
      // 返回空
      return null;
    } catch (Exception e) {
      throw new BuilderException("Could not find value method on SQL annotation.  Cause: " + e, e);
    }
  }

  /**
   * 创建 SqlSource 对象
   */
  private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
    // 拼接 SQL
    final StringBuilder sql = new StringBuilder();
    for (String fragment : strings) {
      sql.append(fragment);
      // 这里只以空格分割，也就是如果有多条SQL的话， fragment 本身必须以 ';' 结尾
      sql.append(" ");
    }
    // 创建 SqlSource 对象，实现逻辑 在 script 模块
    return languageDriver.createSqlSource(configuration, sql.toString().trim(), parameterTypeClass);
  }

  /**
   * 获得方法对应的 SQL 命令类型
   */
  private SqlCommandType getSqlCommandType(Method method) {
    // 获得符合 SQL_ANNOTATION_TYPES 类型的注解类型
    Class<? extends Annotation> type = getSqlAnnotationType(method);

    if (type == null) {
      // 获得符合 SQL_PROVIDER_ANNOTATION_TYPES 类型的注解类型
      type = getSqlProviderAnnotationType(method);

      // 找不到，返回 SqlCommandType.UNKNOWN
      if (type == null) {
        return SqlCommandType.UNKNOWN;
      }

      // 转换成对应的枚举，即使是Provider，转成对应的注解
      if (type == SelectProvider.class) {
        type = Select.class;
      } else if (type == InsertProvider.class) {
        type = Insert.class;
      } else if (type == UpdateProvider.class) {
        type = Update.class;
      } else if (type == DeleteProvider.class) {
        type = Delete.class;
      }
    }

    // 转换成对应的枚举
    return SqlCommandType.valueOf(type.getSimpleName().toUpperCase(Locale.ENGLISH));
  }

  /**
   * 获得符合指定类型的注解类型
   *
   * @param method 方法
   * @return 查到的注解类型
   */
  private Class<? extends Annotation> getSqlAnnotationType(Method method) {
    return chooseAnnotationType(method, SQL_ANNOTATION_TYPES);
  }

  /**
   * 获得方法上的 SQL_ANNOTATION_TYPES 类型的注解类型
   */
  private Class<? extends Annotation> getSqlProviderAnnotationType(Method method) {
    return chooseAnnotationType(method, SQL_PROVIDER_ANNOTATION_TYPES);
  }

  /**
   * 选择方法上的注解类型，要求 types 里面有才行
   */
  private Class<? extends Annotation> chooseAnnotationType(Method method, Set<Class<? extends Annotation>> types) {
    for (Class<? extends Annotation> type : types) {
      Annotation annotation = method.getAnnotation(type);
      if (annotation != null) {
        return type;
      }
    }
    return null;
  }

  /**
   * 将 @Result[] 注解数组，解析成对应的 ResultMapping 对象们，并添加到 resultMappings 中
   */
  private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
    // 遍历 @Result[] 数组
    for (Result result : results) {
      // 创建 ResultFlag 数组
      List<ResultFlag> flags = new ArrayList<>();
      if (result.id()) {
        flags.add(ResultFlag.ID);
      }
      @SuppressWarnings("unchecked")
      // 获得 TypeHandler 类
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              ((result.typeHandler() == UnknownTypeHandler.class) ? null : result.typeHandler());
      // 构建 ResultMapping 对象
      ResultMapping resultMapping = assistant.buildResultMapping(
          resultType,
          nullOrEmpty(result.property()),
          nullOrEmpty(result.column()),
          result.javaType() == void.class ? null : result.javaType(),
          result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
          hasNestedSelect(result) ? nestedSelectId(result) : null,
          null,
          null,
          null,
          typeHandler,
          flags,
          null,
          null,
          isLazy(result));
      // 添加到 resultMappings 中
      resultMappings.add(resultMapping);
    }
  }

  /**
   * 获得内嵌的查询编号
   */
  private String nestedSelectId(Result result) {
    // 先获得 @One 注解
    String nestedSelect = result.one().select();
    // 获得不到，则再获得 @Many
    if (nestedSelect.length() < 1) {
      nestedSelect = result.many().select();
    }
    // 获得内嵌查询编号，格式为 `{type.name}.${select}`
    if (!nestedSelect.contains(".")) {
      nestedSelect = type.getName() + "." + nestedSelect;
    }
    return nestedSelect;
  }

  /**
   * 判断是否懒加载
   * 注解上有 @one，@many 才会去判断
   */
  private boolean isLazy(Result result) {
    // 判断是否开启懒加载
    boolean isLazy = configuration.isLazyLoadingEnabled();
    // 如果有 @One 注解，则判断是否懒加载
    if (result.one().select().length() > 0 && FetchType.DEFAULT != result.one().fetchType()) {
      isLazy = result.one().fetchType() == FetchType.LAZY;
    // 如果有 @Many 注解，则判断是否懒加载
    } else if (result.many().select().length() > 0 && FetchType.DEFAULT != result.many().fetchType()) {
      isLazy = result.many().fetchType() == FetchType.LAZY;
    }
    return isLazy;
  }

  /**
   * 判断是否有内嵌的查询
   */
  private boolean hasNestedSelect(Result result) {
    if (result.one().select().length() > 0 && result.many().select().length() > 0) {
      throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
    }
    // 判断有 @One 或 @Many 注解
    return result.one().select().length() > 0 || result.many().select().length() > 0;  
  }

  /**
   * 将 @Arg[] 注解数组，解析成对应的 ResultMapping 对象们，并添加到 resultMappings 中
   */
  private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings) {
    // 遍历 @Arg[] 数组
    for (Arg arg : args) {
      // 创建 ResultFlag 数组
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if (arg.id()) {
        flags.add(ResultFlag.ID);
      }
      @SuppressWarnings("unchecked")
      // 获得 TypeHandler
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              (arg.typeHandler() == UnknownTypeHandler.class ? null : arg.typeHandler());
      // 将当前 @Arg 注解构建成 ResultMapping 对象
      ResultMapping resultMapping = assistant.buildResultMapping(
          resultType,
          nullOrEmpty(arg.name()),
          nullOrEmpty(arg.column()),
          arg.javaType() == void.class ? null : arg.javaType(),
          arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(),
          nullOrEmpty(arg.select()),
          nullOrEmpty(arg.resultMap()),
          null,
          nullOrEmpty(arg.columnPrefix()),
          typeHandler,
          flags,
          null,
          null,
          false);
      // 添加到 resultMappings 中
      resultMappings.add(resultMapping);
    }
  }

  /**
   * 如果字符串是空字符串或者为空，返回null
   */
  private String nullOrEmpty(String value) {
    return value == null || value.trim().length() == 0 ? null : value;
  }

  /**
   * 判断 @Results 的 Result[] 属性是否设置，没设置的话，这里设置默认值为空数组。TODO 默认值不就是空数组吗？为毛这里还再去重新设置
   */
  private Result[] resultsIf(Results results) {
    return results == null ? new Result[0] : results.value();
  }

  /**
   * 判断 @ConstructorArgs 的 Arg[] 属性是否设置，没设置的话，这里设置默认值为空数组。TODO 默认值不就是空数组吗？为毛这里还再去重新设置
   */
  private Arg[] argsIf(ConstructorArgs args) {
    return args == null ? new Arg[0] : args.value();
  }

  /**
   * 处理 @SelectKey 注解，生成对应的 SelectKey 对象
   */
  private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
    // 获得各种属性和对应的类
    String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    Class<?> resultTypeClass = selectKeyAnnotation.resultType();
    StatementType statementType = selectKeyAnnotation.statementType();
    String keyProperty = selectKeyAnnotation.keyProperty();
    String keyColumn = selectKeyAnnotation.keyColumn();
    boolean executeBefore = selectKeyAnnotation.before();

    // 创建 MappedStatement 需要用到的默认值
    boolean useCache = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;

    // 创建 SqlSource 对象
    SqlSource sqlSource = buildSqlSourceFromStrings(selectKeyAnnotation.statement(), parameterTypeClass, languageDriver);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;

    // 创建 MappedStatement 对象
    assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum,
        flushCache, useCache, false,
        keyGenerator, keyProperty, keyColumn, null, languageDriver, null);

    // 获得 SelectKeyGenerator 的编号，格式为 `${namespace}.${id}`
    id = assistant.applyCurrentNamespace(id, false);
    // 获得 MappedStatement 对象
    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    // 创建 SelectKeyGenerator 对象，并添加到 configuration 中
    SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
    configuration.addKeyGenerator(id, answer);
    return answer;
  }

}
