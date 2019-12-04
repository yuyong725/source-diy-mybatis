package cn.javadog.sd.mybatis.support.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author: 余勇
 * @date: 2019-12-04 20:14
 *
 * 实现 Cache 接口，同步的 Cache 实现类；就是对所有方法枷锁，实现都是交给委托对象
 */
public class SynchronizedCache implements Cache {

  /**
   * 委托的 Cache 对象
   */
  private final Cache delegate;

  /**
   * 构造
   */
  public SynchronizedCache(Cache delegate) {
    this.delegate = delegate;
  }

  // 👇方法都是直接使用的委托对象去做，只是都加了synchronized

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public synchronized int getSize() {
    return delegate.getSize();
  }

  @Override
  public synchronized void putObject(Object key, Object object) {
    delegate.putObject(key, object);
  }

  @Override
  public synchronized Object getObject(Object key) {
    return delegate.getObject(key);
  }

  @Override
  public synchronized Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public synchronized void clear() {
    delegate.clear();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

}
