package cn.javadog.sd.mybatis.support.cache.decorators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author 余勇
 * @date 2019-12-04 16:12
 *
 * 实现 Cache 接口，基于先进先出的淘汰机制的 Cache 实现类
 */
public class FifoCache implements Cache {

  /**
   * 委托的 Cache 对象
   */
  private final Cache delegate;

  /**
   * 双端队列，记录缓存键的添加
   */
  private final Deque<Object> keyList;

  /**
   * 队列上限，这个size不是delegate的size，但会对它起限制
   */
  private int size;

  /**
   * 构造
   */
  public FifoCache(Cache delegate) {
    this.delegate = delegate;
    // 使用了 LinkedList
    this.keyList = new LinkedList<>();
    // 默认为 1024
    this.size = 1024;
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
   * 设置keyList的size
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * 添加缓存
   */
  @Override
  public void putObject(Object key, Object value) {
    // 循环 keyList
    cycleKeyList(key);
    // 添加到缓存
    delegate.putObject(key, value);
  }

  /**
   * 获取指定key的缓存
   */
  @Override
  public Object getObject(Object key) {
    return delegate.getObject(key);
  }

  /**
   * 移除缓存
   * 这里没有移除 keyList，FIFO逻辑在{@link #cycleKeyList(Object)}里面，并不是很严格的实现
   */
  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  /**
   * 清除缓存
   */
  @Override
  public void clear() {
    delegate.clear();
    keyList.clear();
  }

  /**
   * 获取读写锁，这里没做实现
   */
  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * FIFO的核心实现，循环链表
   * 注意这里没有去重
   */
  private void cycleKeyList(Object key) {
    // 添加到 keyList 对位
    keyList.addLast(key);
    // 超过上限，将队首位移除
    if (keyList.size() > size) {
      Object oldestKey = keyList.removeFirst();
      // 移除掉首位
      delegate.removeObject(oldestKey);
    }
  }

}
