package cn.javadog.sd.mybatis.support.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.exceptions.CacheException;

/**
 * Simple blocking decorator 
 * 
 * Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 * 
 * @author Eduardo Macarron
 *
 *
 */
/**
 * @author: 余勇
 * @date: 2019-12-04 15:28
 * 实现 Cache 接口，阻塞的 Cache 实现类。
 * 这是一个简单低效版本的 Cache 阻塞装饰器
 */
public class BlockingCache implements Cache {

  /**
   * 阻塞等待超时时间，通过set设置，没设置就是默认0
   */
  private long timeout;

  /**
   * 委托的 Cache 对象
   */
  private final Cache delegate;

  /**
   * 缓存键与 ReentrantLock 对象的映射
   */
  private final ConcurrentHashMap<Object, ReentrantLock> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
  }

  /**
   * 获取缓存标示，交给委托
   */
  @Override
  public String getId() {
    return delegate.getId();
  }

  /**
   * 获取缓存map存了多少，交给委托
   */
  @Override
  public int getSize() {
    return delegate.getSize();
  }

  /**
   * 添加缓存
   * 添加成功后，会释放指定键的锁
   */
  @Override
  public void putObject(Object key, Object value) {
    try {
      delegate.putObject(key, value);
    } finally {
      releaseLock(key);
    }
  }

  /**
   * 获取指定键的缓存
   */
  @Override
  public Object getObject(Object key) {
    // 获得锁
    acquireLock(key);
    // 获得缓存值
    Object value = delegate.getObject(key);
    // 如果从缓存里面拿到了，就释放锁，否则就不放
    if (value != null) {
      releaseLock(key);
    }        
    return value;
  }

  /**
   * 移除键，别看这玩意名字，其实键没移除，只释放了锁而已，
   * 这个要与 {@link TransactionalCache} 搭配一起看，TransactionalCache 的removeObject啥事都没干，也就是说如果TransactionalCache
   * 装饰了BlockingCache，回滚的时候会调用，这个时候缓存都没有加进去，更谈不上删除缓存，所以只是释放锁。
   * 不搭配使用的话，BlockingCache 不少逻辑是不严谨的
   */
  @Override
  public Object removeObject(Object key) {
    // 释放锁
    releaseLock(key);
    return null;
  }

  /**
   * 清楚缓存
   */
  @Override
  public void clear() {
    delegate.clear();
  }

  /**
   * 获取读写锁，没有实现
   */
  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * 获得 ReentrantLock 对象。如果不存在，进行添加
   * note 逻辑很简单，也很有意思，看基础牢不牢固。乍一看，会觉得逻辑冗余，直接 return locks.putIfAbsent(key, new ReentrantLock());不就好了
   *  实际 putIfAbsent 如果真的absent，返回的是null。为了效率可以这样
   *
   * <pre>
   *    ReentrantLock previous = locks.get(key);
   *    if (previous == null) {
   *      previous = new ReentrantLock();
   *      locks.put(key, previous);
   *    }
   *    return previous;
   * </pre>
   *
   * @param key 缓存键
   * @return ReentrantLock 对象
   */
  private ReentrantLock getLockForKey(Object key) {
    ReentrantLock lock = new ReentrantLock();
    ReentrantLock previous = locks.putIfAbsent(key, lock);
    return previous == null ? lock : previous;
  }

  /**
   * 获取锁
   */
  private void acquireLock(Object key) {
    // 获得 ReentrantLock 对象。
    Lock lock = getLockForKey(key);
    if (timeout > 0) {
      try {
        // 获得锁，直到超时，这里阻塞是因为别的线程getObject阻塞了，这时候要么这个线程get完拿到结果释放，要么其他的线程putObject释放，
        // 亦或者回滚调用removeObject释放，不染就凉凉
        boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        if (!acquired) {
          // 没获取到抛出异常CacheException
          throw new CacheException("Couldn't get a lock in " + timeout + " for the key " +  key + " at the cache " + delegate.getId());
        }
      } catch (InterruptedException e) {
        // 线程被其他原因打断也抛出异常
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    } else {
      // 这种情况就是没有设置timeout，不等待，拿不到就直接休眠，直到被唤醒
      lock.lock();
    }
  }

  /**
   * 释放锁
   */
  private void releaseLock(Object key) {
    // 获得 ReentrantLock 对象
    ReentrantLock lock = locks.get(key);
    // 如果当前线程持有，进行释放！
    if (lock.isHeldByCurrentThread()) {
      lock.unlock();
    }
  }

  /**
   * 获取超时时间
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * 设置超时时间
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }  
}