package cn.javadog.sd.mybatis.executor;

import java.sql.SQLException;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.cache.CacheKey;
import cn.javadog.sd.mybatis.support.cache.TransactionalCacheManager;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.transaction.Transaction;

/**
 * @author 余勇
 * @date 2019-12-16 21:15
 * 实现 Executor 接口，支持二级缓存的 Executor 的实现类。
 * note 与其他执行器不同，该执行器直接继承 Executor，自己管理实务。可以理解为这是装饰模式
 *
 */
public class CachingExecutor implements Executor {

  /**
   * 被委托的 Executor 对象
   */
  private final Executor delegate;

  /**
   * TransactionalCacheManager 对象
   */
  private final TransactionalCacheManager tcm = new TransactionalCacheManager();

  /**
   * 构造函数
   */
  public CachingExecutor(Executor delegate) {
    this.delegate = delegate;
    // 设置 delegate 被当前执行器所包装
    delegate.setExecutorWrapper(this);
  }

  /**
   * 获取 事务
   */
  @Override
  public Transaction getTransaction() {
    return delegate.getTransaction();
  }

  /**
   * 关闭执行器
   */
  @Override
  public void close(boolean forceRollback) {
    try {
      //issues #499, #524 and #573
      if (forceRollback) {
        tcm.rollback();
      }
      // 如果强制提交，则提交 TransactionalCacheManager
      else {
        tcm.commit();
      }
    } finally {
      // 执行 delegate 对应的方法
      delegate.close(forceRollback);
    }
  }

  /**
   * 执行器是否已关闭
   */
  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  /**
   * 执行更新操作
   */
  @Override
  public int update(MappedStatement ms, Object parameterObject) throws SQLException {
    // 如果需要清空缓存，则进行清空
    flushCacheIfRequired(ms);
    // 执行 delegate 对应的方法
    return delegate.update(ms, parameterObject);
  }

  /**
   * 执行查询
   */
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    // 获得 BoundSql 对象
    BoundSql boundSql = ms.getBoundSql(parameterObject);
    // 创建 CacheKey 对象
    CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
    // 查询
    return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  /**
   * 执行查询
   */
  @Override
  public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
    // 如果需要清空缓存，则进行清空
    flushCacheIfRequired(ms);
    // 执行 delegate 对应的方法
    return delegate.queryCursor(ms, parameter, rowBounds);
  }

  /**
   * 执行查询
   */
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {
    // 获取
    Cache cache = ms.getCache();
    if (cache != null) {
      // 如果需要清空缓存，则进行清空
      flushCacheIfRequired(ms);
      // 如果使用缓存且 resultHandler 为空(因为resultHandler能够更改取出来的值)
      if (ms.isUseCache() && resultHandler == null) {
        @SuppressWarnings("unchecked")
        // 从二级缓存中，获取结果
        List<E> list = (List<E>) tcm.getObject(cache, key);
        if (list == null) {
          // 如果不存在，则从数据库中查询
          list = delegate.query(ms, parameterObject, rowBounds, null, key, boundSql);
          // 缓存结果到二级缓存中，可以看看 issue #578 and #116。note 如果list是bull，就会造成缓存穿透
          tcm.putObject(cache, key, list);
        }
        // 如果存在，则直接返回结果
        return list;
      }
    }
    // 不使用缓存，则从数据库中查询
    return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  /**
   * 批处理
   */
  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return delegate.flushStatements();
  }

  /**
   * 提交
   */
  @Override
  public void commit(boolean required) throws SQLException {
    // 执行 delegate 对应的方法
    delegate.commit(required);
    // 提交 TransactionalCacheManager
    tcm.commit();
  }

  /**
   * 回滚
   */
  @Override
  public void rollback(boolean required) throws SQLException {
    try {
      // 执行 delegate 对应的方法
      delegate.rollback(required);
    } finally {
      if (required) {
        // 回滚 TransactionalCacheManager
        tcm.rollback();
      }
    }
  }

  /**
   * 创建缓存对象
   */
  @Override
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
  }

  @Override
  public boolean isCached(MappedStatement ms, CacheKey key) {
    return delegate.isCached(ms, key);
  }

  @Override
  public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
    delegate.deferLoad(ms, resultObject, property, key, targetType);
  }

  @Override
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  /**
   * 如果需要清空缓存，则进行清空
   */
  private void flushCacheIfRequired(MappedStatement ms) {
    Cache cache = ms.getCache();
    // isFlushCacheRequired 取决于SQL的类型，查不刷新，增删改刷新
    if (cache != null && ms.isFlushCacheRequired()) {      
      tcm.clear(cache);
    }
  }

  /**
   * 不做实现，也就是说不能自己包装该缓存类
   */
  @Override
  public void setExecutorWrapper(Executor executor) {
    throw new UnsupportedOperationException("This method should not be called");
  }

}
