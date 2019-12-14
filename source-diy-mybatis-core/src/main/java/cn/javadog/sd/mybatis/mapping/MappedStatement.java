package cn.javadog.sd.mybatis.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  private String resource;

  /**
   * 全局配置
   */
  private Configuration configuration;

  /**
   * 唯一标示
   */
  private String id;

  /**
   * 驱动每次返回的行数，比如查询一句符合条件1000条，但fetchSize设置的额200条，那么驱动会将
   * 查询到的结果分五次返回，当然，客户端是无感知的。哈哈，我猜的😄
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
   */
  private Cache cache;

  /**
   * 对应的 ParameterMap
   */
  private ParameterMap parameterMap;

  /**
   * 对应的 ResultMap 列表，一个SQL可以根据结果，使用不同的 ResultMap
   */
  private List<ResultMap> resultMaps;

  /**
   * 是否需要刷新缓存，这取决于sql 的类型和缓存的类型
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
   * 数据库ID
   */
  private String databaseId;

  /**
   * 日志打印器
   */
  private Log statementLog;

  /**
   * 语言驱动
   */
  private LanguageDriver lang;

  /**
   * 用于存储过程，返回多个结果集
   */
  private String[] resultSets;

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
      // parameterMap 使用ParameterMap.Builder构建一个默认的实现
      mappedStatement.parameterMap = new ParameterMap.Builder(configuration, "defaultParameterMap", null, new ArrayList<>()).build();
      // resultMaps 空数组
      mappedStatement.resultMaps = new ArrayList<>();
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

    /**
     * 设置 parameterMap
     */
    public Builder parameterMap(ParameterMap parameterMap) {
      mappedStatement.parameterMap = parameterMap;
      return this;
    }

    /**
     * 设置 resultMaps
     */
    public Builder resultMaps(List<ResultMap> resultMaps) {
      mappedStatement.resultMaps = resultMaps;
      for (ResultMap resultMap : resultMaps) {
        // 只要一个resultMap有嵌套的resultMap，就可以认为 mappedStatement 有嵌套查询
        mappedStatement.hasNestedResultMaps = mappedStatement.hasNestedResultMaps || resultMap.hasNestedResultMaps();
      }
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
     * 设置 databaseId
     */
    public Builder databaseId(String databaseId) {
      mappedStatement.databaseId = databaseId;
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
     * 设置 resultSets
     */
    public Builder resultSets(String resultSet) {
      // 将 resultSet 切割成数组
      mappedStatement.resultSets = delimitedStringToArray(resultSet);
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
      // 将 resultMaps 包装成不可变
      mappedStatement.resultMaps = Collections.unmodifiableList(mappedStatement.resultMaps);
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

  public ParameterMap getParameterMap() {
    return parameterMap;
  }

  public List<ResultMap> getResultMaps() {
    return resultMaps;
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

  public String getDatabaseId() {
    return databaseId;
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

  public String[] getResultSets() {
    return resultSets;
  }

  /**
   * 非常重要，获取对应的 BoundSql
   */
  public BoundSql getBoundSql(Object parameterObject) {
    // 获得 BoundSql 对象
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    // 忽略，因为 <parameterMap /> 已经废弃，参见 http://www.mybatis.org/mybatis-3/zh/sqlmap-xml.html 文档
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (parameterMappings == null || parameterMappings.isEmpty()) {
      boundSql = new BoundSql(configuration, boundSql.getSql(), parameterMap.getParameterMappings(), parameterObject);
    }
    // 判断传入的参数中，是否有内嵌的结果 ResultMap 。如果有，则修改 hasNestedResultMaps 为 true
    // 存储过程相关，暂时无视
    for (ParameterMapping pm : boundSql.getParameterMappings()) {
      // 拿到ParameterMapping的ResultMapId，因为此参数对应返回值，需要解析。只有存储过程才会由此属性
      String rmId = pm.getResultMapId();
      if (rmId != null) {
        // 拿到对应的resultMap
        ResultMap rm = configuration.getResultMap(rmId);
        if (rm != null) {
          // |= 挺有意思，a |= b 相当于 a= a || b
          hasNestedResultMaps |= rm.hasNestedResultMaps();
        }
      }
    }

    return boundSql;
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
