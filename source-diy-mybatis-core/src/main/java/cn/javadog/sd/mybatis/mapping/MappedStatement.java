package cn.javadog.sd.mybatis.mapping;

import java.util.ArrayList;

import cn.javadog.sd.mybatis.executor.keygen.Jdbc3KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.NoKeyGenerator;
import cn.javadog.sd.mybatis.scripting.LanguageDriver;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * @author 余勇
 * @date 2019-12-13 20:38
 *
 * 映射的语句，每个 <select />、<insert />、<update />、<delete /> 对应一个 MappedStatement 对象
 * 比较特殊的是，<selectKey /> 解析后，也会对应一个 MappedStatement 对象
 */
public final class MappedStatement {

  /**
   * 对应的xml资源, 如：org/apache/ibatis/builder/AuthorMapper.xml
   * 对应的mapper接口，如：org/apache/ibatis/domain/blog/mappers/AuthorMapper.java (best guess)
   */
  private String resource;

  /**
   * 全局配置
   */
  private Configuration configuration;

  /**
   * 唯一标示。
   * ${namespace}.${methodname}，
   * 如: cn.javadog.sd.mybatis.example.mapper.AuthorMapper.selectAllAuthorsArray
   */
  private String id;

  /**
   * 驱动每次返回的行数，比如查询一句符合条件1000条，但fetchSize设置的额200条，那么驱动会将
   * 查询到的结果分五次返回，当然，客户端是无感知的。
   * 补充阅读：https://juejin.im/post/5a6757e351882573541c86bb
   */
  private Integer fetchSize;

  /**
   * 数据库连接超时时间
   */
  private Integer timeout;

  /**
   * statement的类型，参见 {@link StatementType}
   */
  private StatementType statementType;

  /**
   * ResultSet的类型，参见 {@link ResultSetType}。
   * 代表的是读取数据库返回结果的方式，比如返回100条，扔在一块缓存的区域，使用游标去读，这时候数据变了，读取到的结果变不变
   */
  private ResultSetType resultSetType;

  /**
   * 关联的 SqlSource
   */
  private SqlSource sqlSource;

  /**
   * Cache 对象，用于二级缓存
   * TODO 一级还是二级？
   */
  private Cache cache;

  /**
   * 对应的 ResultMap 列表，一个SQL可以根据结果，使用不同的 ResultMap
   */
  private ResultMap resultMap;

  /**
   * 是否需要刷新缓存，这取决于sql 的类型和缓存的类型.
   * 从赋值的逻辑可以看到，查询不刷新，增改删除会刷新
   */
  private boolean flushCacheRequired;

  /**
   * 是否使用 一级缓存
   */
  private boolean useCache;

  /**
   * 是否对结果进行排序。TODO 貌似这与 Cursor 有关
   */
  private boolean resultOrdered;

  /**
   * SQL命令的类型，参见 {@link SqlCommandType}
   */
  private SqlCommandType sqlCommandType;

  /**
   * 主键生成器策略，一般用于读取数据库生成的主键，反写到实体类
   */
  private KeyGenerator keyGenerator;

  /**
   * 主键对应的字段名
   */
  private String[] keyProperties;

  /**
   * 主键对应的列名
   */
  private String[] keyColumns;

  /**
   * 是否有嵌套查询
   */
  private boolean hasNestedResultMaps;

  /**
   * 日志打印器
   */
  private Log statementLog;

  /**
   * 语言驱动
   */
  private LanguageDriver lang;

  /**
   * 关联的 parameterMap，此框架只有内联和无参数的 parameterMap
   */
  private ParameterMap parameterMap;

  /**
   * 构造函数，没被public修改，相当于关闭了，由 构造器调用暴露
   */
  MappedStatement() {
  }

  /**
   * 内部类，MappedStatement的构造器
   */
  public static class Builder {

    /**
     * 要构建的 mappedStatement 对象，空属性
     */
    private MappedStatement mappedStatement = new MappedStatement();

    /**
     * 构造函数
     */
    public Builder(Configuration configuration, String id, SqlSource sqlSource, SqlCommandType sqlCommandType) {
      mappedStatement.configuration = configuration;
      mappedStatement.id = id;
      mappedStatement.sqlSource = sqlSource;
      // statementType使用默认值，PREPARED
      mappedStatement.statementType = StatementType.PREPARED;
      // resultSetType使用默认值 DEFAULT
      mappedStatement.resultSetType = ResultSetType.DEFAULT;
      mappedStatement.parameterMap = new ParameterMap.Builder(configuration, "defaultParameterMap", null, new ArrayList<>()).build();
      mappedStatement.sqlCommandType = sqlCommandType;
      // 当sql命令是insert操作，且全局配置需要生成主键，就使用Jdbc3KeyGenerator(适用于MySQL)，否则使用NoKeyGenerator，也就是不做操作
      mappedStatement.keyGenerator = configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType) ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
      String logId = id;
      if (configuration.getLogPrefix() != null) {
        logId = configuration.getLogPrefix() + id;
      }
      // 初始化 statementLog
      mappedStatement.statementLog = LogFactory.getLog(logId);
      // 使用全局默认的语言驱动
      mappedStatement.lang = configuration.getDefaultScriptingLanguageInstance();
    }

    /**
     * 设置 resource
     */
    public Builder resource(String resource) {
      mappedStatement.resource = resource;
      return this;
    }

    /**
     * 获取mappedStatement的唯一标示
     */
    public String id() {
      return mappedStatement.id;
    }

    public Builder parameterMap(ParameterMap parameterMap) {
      mappedStatement.parameterMap = parameterMap;
      return this;
    }

    /**
     * 设置 resultMap
     */
    public Builder resultMap(ResultMap resultMap) {
      mappedStatement.resultMap = resultMap;
      // 只要一个resultMap有嵌套的resultMap，就可以认为 mappedStatement 有嵌套查询
      mappedStatement.hasNestedResultMaps = mappedStatement.hasNestedResultMaps || resultMap.hasNestedResultMaps();
      return this;
    }

    /**
     * 设置 fetchSize
     */
    public Builder fetchSize(Integer fetchSize) {
      mappedStatement.fetchSize = fetchSize;
      return this;
    }

    /**
     * 设置 timeout
     */
    public Builder timeout(Integer timeout) {
      mappedStatement.timeout = timeout;
      return this;
    }

    /**
     * 设置 statementType
     */
    public Builder statementType(StatementType statementType) {
      mappedStatement.statementType = statementType;
      return this;
    }

    /**
     * 设置 resultSetType
     */
    public Builder resultSetType(ResultSetType resultSetType) {
      // 为空的话，就使用默认值 DEFAULT
      mappedStatement.resultSetType = resultSetType == null ? ResultSetType.DEFAULT : resultSetType;
      return this;
    }

    /**
     * 设置 缓存对象
     */
    public Builder cache(Cache cache) {
      mappedStatement.cache = cache;
      return this;
    }

    /**
     * 设置 flushCacheRequired
     */
    public Builder flushCacheRequired(boolean flushCacheRequired) {
      mappedStatement.flushCacheRequired = flushCacheRequired;
      return this;
    }

    /**
     * 设置 useCache
     */
    public Builder useCache(boolean useCache) {
      mappedStatement.useCache = useCache;
      return this;
    }

    /**
     * 设置 resultOrdered
     */
    public Builder resultOrdered(boolean resultOrdered) {
      mappedStatement.resultOrdered = resultOrdered;
      return this;
    }

    /**
     * 设置 keyGenerator
     */
    public Builder keyGenerator(KeyGenerator keyGenerator) {
      mappedStatement.keyGenerator = keyGenerator;
      return this;
    }

    public Builder keyProperty(String keyProperty) {
      mappedStatement.keyProperties = delimitedStringToArray(keyProperty);
      return this;
    }

    public Builder keyColumn(String keyColumn) {
      mappedStatement.keyColumns = delimitedStringToArray(keyColumn);
      return this;
    }

    /**
     * 设置 lang
     */
    public Builder lang(LanguageDriver driver) {
      mappedStatement.lang = driver;
      return this;
    }

    /**
     * 执行构建，就是一些断言。
     * note 这个构建和 {@link Discriminator.Builder#build()} 大不相同。这里的每一个链式方法实打实改的是 mappedStatement 的值，
     * 构建方法只是做了断言
     */
    public MappedStatement build() {
      assert mappedStatement.configuration != null;
      assert mappedStatement.id != null;
      assert mappedStatement.sqlSource != null;
      assert mappedStatement.lang != null;
      assert mappedStatement.resultMap != null;
      return mappedStatement;
    }
  }

  /*所有属性的get方法*/

  public KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  public SqlCommandType getSqlCommandType() {
    return sqlCommandType;
  }

  public String getResource() {
    return resource;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public ParameterMap getParameterMap() {
    return parameterMap;
  }

  public String getId() {
    return id;
  }

  public boolean hasNestedResultMaps() {
    return hasNestedResultMaps;
  }

  public Integer getFetchSize() {
    return fetchSize;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public StatementType getStatementType() {
    return statementType;
  }

  public ResultSetType getResultSetType() {
    return resultSetType;
  }

  public SqlSource getSqlSource() {
    return sqlSource;
  }

  public ResultMap getResultMap() {
    return resultMap;
  }

  public Cache getCache() {
    return cache;
  }

  public boolean isFlushCacheRequired() {
    return flushCacheRequired;
  }

  public boolean isUseCache() {
    return useCache;
  }

  public boolean isResultOrdered() {
    return resultOrdered;
  }

  public String[] getKeyProperties() {
    return keyProperties;
  }

  public String[] getKeyColumns() {
    return keyColumns;
  }

  public Log getStatementLog() {
    return statementLog;
  }

  public LanguageDriver getLang() {
    return lang;
  }

  /**
   * 非常重要，获取对应的 BoundSql
   */
  public BoundSql getBoundSql(Object parameterObject) {
    // 获得 BoundSql 对象
    return sqlSource.getBoundSql(parameterObject);
  }

  /**
   * 将字符串以 ',' 切割成数组
   */
  private static String[] delimitedStringToArray(String in) {
    if (in == null || in.trim().length() == 0) {
      return null;
    } else {
      return in.split(",");
    }
  }

}
