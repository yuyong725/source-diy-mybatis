package cn.javadog.sd.mybatis.support.cache;

import java.util.HashMap;
import java.util.Map;

import cn.javadog.sd.mybatis.support.cache.decorators.TransactionalCache;

/**
 * @author: 余勇
 * @date: 2019-12-04 21:53
 *
 * TransactionalCache 管理器。
 */
public class TransactionalCacheManager {

  /**
   * Cache 和 TransactionalCache 的映射
   */
  private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();

  /**
   * 清除某个缓存
   */
  public void clear(Cache cache) {
    getTransactionalCache(cache).clear();
  }

  /**
   * 获得缓存中，指定 Cache + K 的值
   */
  public Object getObject(Cache cache, CacheKey key) {
    // 首先，获得 Cache 对应的 TransactionalCache 对象
    // 然后从 TransactionalCache 对象中，获得 key 对应的值
    return getTransactionalCache(cache).getObject(key);
  }

  /**
   * 添加 Cache + KV ，到缓存中
   */
  public void putObject(Cache cache, CacheKey key, Object value) {
    // 首先，获得 Cache 对应的 TransactionalCache 对象
    // 然后，添加 KV 到 TransactionalCache 对象中
    getTransactionalCache(cache).putObject(key, value);
  }

  /**
   * 提交所有 TransactionalCache
   */
  public void commit() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      txCache.commit();
    }
  }

  /**
   * 回滚所有 TransactionalCache
   */
  public void rollback() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      txCache.rollback();
    }
  }

  /**
   * 获取cache对应的TransactionalCache对象，就是对cache进行装饰，增加事务支持
   */
  private TransactionalCache getTransactionalCache(Cache cache) {
    return transactionalCaches.computeIfAbsent(cache, TransactionalCache::new);
  }

}
