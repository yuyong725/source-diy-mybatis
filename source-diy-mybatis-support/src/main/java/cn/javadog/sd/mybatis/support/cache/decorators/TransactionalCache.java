package cn.javadog.sd.mybatis.support.cache.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * The 2nd level cache transactional buffer.
 * 
 * This class holds all cache entries that are to be added to the 2nd level cache during a Session.
 * Entries are sent to the cache when commit is called or discarded if the Session is rolled back. 
 * Blocking cache support has been added. Therefore any get() that returns a cache miss will be followed by a put()
 * so any lock associated with the key can be released.
 * 
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 *
 */
/**
 * @author 余勇
 * @date 2019-12-04 20:20
 *
 * 支持事务的 Cache 实现类，主要用于二级缓存中。
 * 在一次会话中，所有添加到二级缓存的缓存都由这个类管理，当会话提交时缓存就会添加进来，回滚就会丢弃。
 * 目前也已经支持阻塞式的缓存。因此，任何get()调用都会返回缓存，如果确实，就会调用put()放进去，因此相关的key会被立即释放掉，不会阻塞！
 * TODO put进去的是啥？null的话也没用啊
 */
public class TransactionalCache implements Cache {

  private static final Log log = LogFactory.getLog(TransactionalCache.class);

  /**
   * 委托的 Cache 对象。
   *
   * 实际上，就是二级缓存 Cache 对象。
   */
  private final Cache delegate;

  /**
   * 提交时，是否去清空 {@link #delegate}
   *
   * 初始时，该值为 false
   * 清理后{@link #clear()} 时，该值为 true ，表示持续处于清空状态；
   */
  private boolean clearOnCommit;

  /**
   * 待提交的 KV 映射，没有放入缓存
   */
  private final Map<Object, Object> entriesToAddOnCommit;

  /**
   * 查找不到的 KEY 集合
   */
  private final Set<Object> entriesMissedInCache;

  /**
   * 构造
   */
  public TransactionalCache(Cache delegate) {
    this.delegate = delegate;
    this.clearOnCommit = false;
    this.entriesToAddOnCommit = new HashMap<>();
    this.entriesMissedInCache = new HashSet<>();
  }

  /**
   * 获取标示
   */
  @Override
  public String getId() {
    return delegate.getId();
  }

  /**
   * 获取缓存的数量
   */
  @Override
  public int getSize() {
    return delegate.getSize();
  }

  /**
   * 获取缓存
   */
  @Override
  public Object getObject(Object key) {
    // 从 delegate 中获取 key 对应的 value
    Object object = delegate.getObject(key);
    // 如果不存在，则添加到 entriesMissedInCache 中
    if (object == null) {
      entriesMissedInCache.add(key);
    }
    // 如果 clearOnCommit 为 true ，表示处于持续清空状态，则返回 null
    if (clearOnCommit) {
      return null;
    } else {
      // 返回 value
      return object;
    }
  }

  /**
   * 获取读写锁，这里没有实现
   */
  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * 暂存 KV 到 entriesToAddOnCommit
   * 只是暂存
   */
  @Override
  public void putObject(Object key, Object object) {
    // 暂存 KV 到 entriesToAddOnCommit 中
    entriesToAddOnCommit.put(key, object);
  }

  /**
   * 移除缓存，没做实现
   */
  @Override
  public Object removeObject(Object key) {
    return null;
  }

  /**
   * 清空缓存，会将clearOnCommit设置为true
   */
  @Override
  public void clear() {
    // 标记 clearOnCommit 为 true
    clearOnCommit = true;
    // 清空 entriesToAddOnCommit，
    // TODO 但没有清空entriesMissedInCache，因为无论如何，都要通过它去释放锁？
    entriesToAddOnCommit.clear();
  }

  /**
   * 提交事务
   */
  public void commit() {
    // 如果 clearOnCommit 为 true ，则清空 delegate 缓存
    if (clearOnCommit) {
      delegate.clear();
    }
    // 将 entriesToAddOnCommit、entriesMissedInCache 刷入 delegate 中
    flushPendingEntries();
    // 重置
    reset();
  }

  /**
   * 回滚事务
   */
  public void rollback() {
    // 从 delegate 移除出 entriesMissedInCache
    // TODO 按理未提交，是不会刷到委托对象的delegate里面。除非是先 clear => commit => rollback 这么玩才会啊，奇怪，到时候再看内部调用时怎么玩的
    unlockMissedEntries();
    // 重置
    reset();
  }

  /**
   * 重置；
   * 重置要么是提交，要么是回滚；
   * 提交：注意clearOnCommit默认就是false，为true只有是提前clear掉了，既然提前clear了，那自然委托对象delegate的缓存也要clear，否则的话，
   *  正常情况就应该将 entriesToAddOnCommit、entriesMissedInCache 刷入 delegate 中。然后将两者清空
   */
  private void reset() {
    // 重置 clearOnCommit 为 false
    clearOnCommit = false;
    // 清空 entriesToAddOnCommit、entriesMissedInCache
    entriesToAddOnCommit.clear();
    entriesMissedInCache.clear();
  }

  /**
   * 将 entriesToAddOnCommit、entriesMissedInCache 刷入 delegate 中
   */
  private void flushPendingEntries() {
    // 将 entriesToAddOnCommit 刷入 delegate 中
    for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
      delegate.putObject(entry.getKey(), entry.getValue());
    }
    // 将 entriesMissedInCache 刷入 delegate 中
    for (Object entry : entriesMissedInCache) {
      if (!entriesToAddOnCommit.containsKey(entry)) {
        // 这一把保证了BlockingCache释放锁
        delegate.putObject(entry, null);
      }
    }
  }

  /**
   * 从delegate移除entriesMissedInCache
   */
  private void unlockMissedEntries() {
    for (Object entry : entriesMissedInCache) {
      try {
        // 移除key
        delegate.removeObject(entry);
      } catch (Exception e) {
        log.warn("Unexpected exception while notifiying a rollback to the cache adapter."
            + "Consider upgrading your cache adapter to the latest version.  Cause: " + e);
      }
    }
  }

}
