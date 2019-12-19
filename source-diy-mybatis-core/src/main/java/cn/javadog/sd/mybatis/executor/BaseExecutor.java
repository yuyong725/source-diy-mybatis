package cn.javadog.sd.mybatis.executor;

import static cn.javadog.sd.mybatis.executor.ExecutionPlaceholder.EXECUTION_PLACEHOLDER;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.statement.StatementUtil;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ParameterMode;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.LocalCacheScope;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.cache.CacheKey;
import cn.javadog.sd.mybatis.support.cache.impl.PerpetualCache;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.logging.jdbc.ConnectionLogger;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
/**
 * @author 余勇
 * @date 2019-12-14 22:50
 * Executor 接口的抽象实现类，完成了大部分方法
 */
public abstract class BaseExecutor implements Executor {

  /**
   * 日志打印器
   */
  private static final Log log = LogFactory.getLog(BaseExecutor.class);

  /**
   * 事务对象
   */
  protected Transaction transaction;

  /**
   * 包装的 Executor 对象
   */
  protected Executor wrapper;

  /**
   * DeferredLoad( 延迟加载 ) 队列
   */
  protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;

  /**
   * 本地缓存，即一级缓存
   */
  protected PerpetualCache localCache;

  /**
   * 全局配置
   */
  protected Configuration configuration;

  /**
   * 记录嵌套查询的层级
   */
  protected int queryStack;

  /**
   * 是否关闭
   */
  private boolean closed;

  /**
   * 构造方法
   */
  protected BaseExecutor(Configuration configuration, Transaction transaction) {
    this.transaction = transaction;
    this.deferredLoads = new ConcurrentLinkedQueue<>();
    // ID是 "LocalCache"，而不是namespace，侧面说明不是存储二级缓存的
    this.localCache = new PerpetualCache("LocalCache");
    // 默认关闭
    this.closed = false;
    this.configuration = configuration;
    this.wrapper = this;
  }

  /**
   * 获得事务对象
   */
  @Override
  public Transaction getTransaction() {
    // 关闭连接了，拿事务就会GG
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    return transaction;
  }

  /**
   * 关闭执行器
   */
  @Override
  public void close(boolean forceRollback) {
    try {
      // 回滚事务
      try {
        rollback(forceRollback);
      } finally {
        // 关闭事务
        if (transaction != null) {
          transaction.close();
        }
      }
    } catch (SQLException e) {
      // 打印个日志就行，不要GG
      log.warn("Unexpected exception on closing transaction.  Cause: " + e);
    } finally {
      // 置空变量
      transaction = null;
      deferredLoads = null;
      localCache = null;
      closed = true;
    }
  }

  /**
   * 查看是否已关闭
   */
  @Override
  public boolean isClosed() {
    return closed;
  }

  /**
   * 执行写操作。主要是记录日志，清空缓存，实现逻辑由子类完成。
   * 很像Spring，方法都分为 method 和 doMethod。前者完成辅助逻辑，如校验，包装，后者则完成核心逻辑
   */
  @Override
  public int update(MappedStatement ms, Object parameter) throws SQLException {
    // 异常上下文先记上一笔
    ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
    // 已经关闭，则抛出 ExecutorException 异常
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 清空本地缓存，更新就清缓存
    clearLocalCache();
    // 执行写操作
    return doUpdate(ms, parameter);
  }

  /**
   * 刷入批处理语句
   */
  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return flushStatements(false);
  }

  /**
   * 刷入批处理语句
   */
  public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
    // 已经关闭，则抛出 ExecutorException 异常
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 执行刷入批处理语句
    return doFlushStatements(isRollBack);
  }

  /**
   * 读操作
   */
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    // 获得 BoundSql 对象
    BoundSql boundSql = ms.getBoundSql(parameter);
    // 创建 CacheKey 对象
    CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
    // 查询
    return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
 }

 /**
  * 读操作
  */
  @SuppressWarnings("unchecked")
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    // 异常上下文先记录一波
    ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
    // 已经关闭，则抛出 ExecutorException 异常
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 清空本地缓存，如果 queryStack 为零，并且要求清空本地缓存。
    if (queryStack == 0 && ms.isFlushCacheRequired()) {
      clearLocalCache();
    }
    List<E> list;
    try {
      // queryStack + 1
      queryStack++;
      // 如果 resultHandler 为空，从一级缓存中，获取查询结果。TODO 为什么resultHandler为空才从缓存拿？
      list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
      // 获取到，则进行处理
      if (list != null) {
        // 处理从本地缓存拿到的结果，源码中有存储过程的处理，这里删去
      } else {
        // 获得不到，则从数据库中查询
        list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
      }
    } finally {
      // queryStack - 1
      queryStack--;
    }
    // TODO 啥玩意这是
    if (queryStack == 0) {
      // 执行延迟加载
      for (DeferredLoad deferredLoad : deferredLoads) {
        deferredLoad.load();
      }
      // 清空 deferredLoads issue #601
      deferredLoads.clear();
      // 如果缓存级别是 LocalCacheScope.STATEMENT ，则进行清理
      if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
        // issue #482 查完就清，没啥鸟用的感觉
        clearLocalCache();
      }
    }
    return list;
  }

  /**
   * 执行查询，返回的结果为 Cursor 游标对象
   */
  @Override
  public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
    // 获得 BoundSql 对象
    BoundSql boundSql = ms.getBoundSql(parameter);
    // 执行查询
    return doQueryCursor(ms, parameter, rowBounds, boundSql);
  }

  /**
   * 延迟加载
   * 详细参考 http://svip.iocoder.cn/MyBatis/executor-5/
   */
  @Override
  public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
    // 已经关了就直接GG
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // new 一个 DeferredLoad
    DeferredLoad deferredLoad = new DeferredLoad(resultObject, property, key, localCache, configuration, targetType);
    // 如果可以立即加载，就立即加载
    if (deferredLoad.canLoad()) {
      deferredLoad.load();
    } else {
      // 否则添加到 deferredLoads
      deferredLoads.add(new DeferredLoad(resultObject, property, key, localCache, configuration, targetType));
    }
  }

  /**
   * 创建 CacheKey 对象
   * TODO 设置那么多值到底干什么
   */
  @Override
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 创建 CacheKey 对象
    CacheKey cacheKey = new CacheKey();
    // 设置 id、offset、limit、sql 到 CacheKey 对象中
    cacheKey.update(ms.getId());
    cacheKey.update(rowBounds.getOffset());
    cacheKey.update(rowBounds.getLimit());
    cacheKey.update(boundSql.getSql());
    // 获取 parameterMappings
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    // 获取 typeHandlerRegistry
    TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
    // 设置 ParameterMapping 数组的元素对应的每个 value 到 CacheKey 对象中
    for (ParameterMapping parameterMapping : parameterMappings) {
      // 这块逻辑，和 DefaultParameterHandler 获取 value 是一致的。
      if (parameterMapping.getMode() != ParameterMode.OUT) {
        Object value;
        String propertyName = parameterMapping.getProperty();
        if (boundSql.hasAdditionalParameter(propertyName)) {
          value = boundSql.getAdditionalParameter(propertyName);
        } else if (parameterObject == null) {
          value = null;
        } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
          value = parameterObject;
        } else {
          MetaObject metaObject = configuration.newMetaObject(parameterObject);
          value = metaObject.getValue(propertyName);
        }
        // 将值设置到cacheKey
        cacheKey.update(value);
      }
    }
    // 置 Environment.id 到 CacheKey 对象中
    if (configuration.getEnvironment() != null) {
      // issue #176
      cacheKey.update(configuration.getEnvironment().getId());
    }
    return cacheKey;
  }

  /**
   * 判断指定key的一级缓存是否存在
   */
  @Override
  public boolean isCached(MappedStatement ms, CacheKey key) {
    return localCache.getObject(key) != null;
  }

  /**
   * 提交
   */
  @Override
  public void commit(boolean required) throws SQLException {
    // 已经关闭，则抛出 ExecutorException 异常
    if (closed) {
      throw new ExecutorException("Cannot commit, transaction is already closed");
    }
    // 清空本地缓存
    clearLocalCache();
    // 刷入批处理语句
    flushStatements();
    // 是否要求提交事务。如果是，则提交事务。
    if (required) {
      transaction.commit();
    }
  }

  /**
   * 回滚
   */
  @Override
  public void rollback(boolean required) throws SQLException {
    if (!closed) {
      try {
        // 清空本地缓存
        clearLocalCache();
        // 刷入批处理语句
        flushStatements(true);
      } finally {
        if (required) {
          // 是否要求回滚事务。如果是，则回滚事务。
          transaction.rollback();
        }
      }
    }
  }

  /**
   * 清理一级（本地）缓存
   */
  @Override
  public void clearLocalCache() {
    if (!closed) {
      // 清理 localCache
      localCache.clear();
    }
  }

  /**
   * 执行更新操作，由子类完成
   */
  protected abstract int doUpdate(MappedStatement ms, Object parameter)
      throws SQLException;

  /**
   * 刷入批处理语句，由子类完成
   */
  protected abstract List<BatchResult> doFlushStatements(boolean isRollback)
      throws SQLException;

  /**
   * 查询，由子类完成
   */
  protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
      throws SQLException;

  /**
   * 查询返回游标的，由子类完成
   */
  protected abstract <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql)
      throws SQLException;

  /**
   * 关闭会话
   */
  protected void closeStatement(Statement statement) {
    if (statement != null) {
      try {
        // 没有关闭就进行关闭
        if (!statement.isClosed()) {
          statement.close();
        }
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  /**
   * 设置事务超时时间
   *
   * @throws SQLException 如果发生SQL异常，会调用 statement 的close方法
   * @since 3.4.0
   * @see StatementUtil#applyTransactionTimeout(Statement, Integer, Integer)
   */
  protected void applyTransactionTimeout(Statement statement) throws SQLException {
    StatementUtil.applyTransactionTimeout(statement, statement.getQueryTimeout(), transaction.getTimeout());
  }


  /**
   * 从数据库中读取操作
   */
  private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    List<E> list;
    // 在缓存中，添加占位对象。此处的占位符，和延迟加载有关，可见 `DeferredLoad#canLoad()` 方法
    localCache.putObject(key, EXECUTION_PLACEHOLDER);
    try {
      // 执行读操作
      list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    } finally {
      // 从缓存中，移除占位对象
      localCache.removeObject(key);
    }
    // 添加到缓存中
    localCache.putObject(key, list);
    return list;
  }

  /**
   *  获得 Connection 代理对象
   */
  protected Connection getConnection(Log statementLog) throws SQLException {
    // 获得 Connection 对象
    Connection connection = transaction.getConnection();
    // 如果 debug 日志级别，则创建 ConnectionLogger 对象，进行动态代理
    if (statementLog.isDebugEnabled()) {
      return ConnectionLogger.newInstance(connection, statementLog, queryStack);
    } else {
      return connection;
    }
  }

  /**
   * 设置包装器
   */
  @Override
  public void setExecutorWrapper(Executor wrapper) {
    this.wrapper = wrapper;
  }

  /**
   * 内部类，记录要延迟记载的内容
   */
  private static class DeferredLoad {

    /**
     * 原对象。如查询到 学生表的信息，学生有个老师字段，需要延迟加载老师信息。则此 resultObject 代表的就是学生信息
     */
    private final MetaObject resultObject;

    /**
     * 延迟加载关联的属性
     */
    private final String property;

    /**
     * 延迟加载的返回对象的类型
     */
    private final Class<?> targetType;

    /**
     * CacheKey 对象
     */
    private final CacheKey key;

    /**
     * 一级缓存
     */
    private final PerpetualCache localCache;

    /**
     * ObjectFactory 对象
     */
    private final ObjectFactory objectFactory;

    /**
     * 结果提取器
     */
    private final ResultExtractor resultExtractor;

    /**
     * 构造函数，参见 issue #781
     */
    public DeferredLoad(MetaObject resultObject,
                        String property,
                        CacheKey key,
                        PerpetualCache localCache,
                        Configuration configuration,
                        Class<?> targetType) {
      this.resultObject = resultObject;
      this.property = property;
      this.key = key;
      this.localCache = localCache;
      // 使用 configuration 里的objectFactory
      this.objectFactory = configuration.getObjectFactory();
      // 初始化 resultExtractor
      this.resultExtractor = new ResultExtractor(configuration, objectFactory);
      this.targetType = targetType;
    }

    /**
     * 是否可以立即加载，也就是缓存中有
     */
    public boolean canLoad() {
      return localCache.getObject(key) != null && localCache.getObject(key) != EXECUTION_PLACEHOLDER;
    }

    /**
     * 立即加载
     */
    public void load() {
      @SuppressWarnings( "unchecked" )
      // 从缓存拿到了列表数据
      List<Object> list = (List<Object>) localCache.getObject(key);
      // 提取结果
      Object value = resultExtractor.extractObjectFromList(list, targetType);
      // 设置到 resultObject
      resultObject.setValue(property, value);
    }

  }

}
