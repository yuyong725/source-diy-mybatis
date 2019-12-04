package cn.javadog.sd.mybatis.support.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author: 余勇
 * @date: 2019-12-04 17:33
 *
 * 实现 Cache 接口，基于最少使用的淘汰机制的 Cache 实现类
 * note 很有学习意义，LRU的真正实践。所谓最少使用可以理解为，是按照最新一次的使用时间进行排序的
 */
public class LruCache implements Cache {

  /**
   * 委托的 Cache 对象
   */
  private final Cache delegate;

  /**
   * 基于 LinkedHashMap 实现淘汰机制
   */
  private Map<Object, Object> keyMap;

  /**
   * 最老的键，即要被淘汰的
   */
  private Object eldestKey;

  /**
   * 构造
   */
  public LruCache(Cache delegate) {
    this.delegate = delegate;
    // 初始化 keyMap 对象
    setSize(1024);
  }

  /**
   * 获取缓存标示
   */
  @Override
  public String getId() {
    return delegate.getId();
  }

  /**
   * 获取已经缓存的数量
   */
  @Override
  public int getSize() {
    return delegate.getSize();
  }

  /**
   * 设置keyMap的大小
   */
  public void setSize(final int size) {
    // LinkedHashMap的一个构造函数，当参数accessOrder为true时，即会按照访问顺序排序，最近访问的放在最前，最早访问的放在后面；为false就按照常见的理解，按加入顺序排序
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {

      private static final long serialVersionUID = 4267176411845948333L;
      // LinkedHashMap自带的判断是否删除最老的元素方法，默认返回false，即不删除老数据
      // 我们要做的就是重写这个方法，当满足一定条件时删除老数据
      @Override
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        // 前面的size()是缓存的数量，后面的size是允许缓存的数量，也是这个方法的参数
        boolean tooBig = size() > size;
        if (tooBig) {
          // 这一步不能忘了，需要做记录
          eldestKey = eldest.getKey();
        }
        return tooBig;
      }
    };
  }

  /**
   * 添加缓存
   */
  @Override
  public void putObject(Object key, Object value) {
    // 添加到缓存
    delegate.putObject(key, value);
    // 循环 keyMap
    cycleKeyList(key);
  }

  /**
   * 获取缓存
   */
  @Override
  public Object getObject(Object key) {
    // 刷新 keyMap 的访问顺序
    keyMap.get(key);
    // 获得缓存值
    return delegate.getObject(key);
  }

  /**
   * 移除缓存
   */
  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  /**
   * 清空缓存
   */
  @Override
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

  /**
   * 获取读写锁，空实现
   */
  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * 循环keyMap，eldestKey只有当达到上限时，才会被记录，然后在这里去掉
   */
  private void cycleKeyList(Object key) {
    // 添加到 keyMap 中
    keyMap.put(key, key);
    // 如果超过上限，则从 delegate 中，移除最少使用的那个
    if (eldestKey != null) {
      delegate.removeObject(eldestKey);
      // 置空
      eldestKey = null;
    }
  }

}
