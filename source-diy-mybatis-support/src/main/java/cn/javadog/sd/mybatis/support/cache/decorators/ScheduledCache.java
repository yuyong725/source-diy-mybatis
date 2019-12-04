package cn.javadog.sd.mybatis.support.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author: 余勇
 * @date: 2019-12-04 20:07
 *
 * 实现 Cache 接口，定时清空整个容器的 Cache 实现类。清空操作并不是定时触发的，而是由相应的动作连带的触发
 */
public class ScheduledCache implements Cache {

  /**
   * 被委托的 Cache 对象
   */
  private final Cache delegate;

  /**
   * 清空间隔，单位：毫秒
   */
  protected long clearInterval;

  /**
   * 最后清空时间，单位：毫秒
   */
  protected long lastClear;

  /**
   * 构造函数
   */
  public ScheduledCache(Cache delegate) {
    this.delegate = delegate;
    // 1 hour
    this.clearInterval = 60 * 60 * 1000;
    this.lastClear = System.currentTimeMillis();
  }

  /**
   * 设置清理的周期
   */
  public void setClearInterval(long clearInterval) {
    this.clearInterval = clearInterval;
  }

  /**
   * 获取命名空间
   */
  @Override
  public String getId() {
    return delegate.getId();
  }

  /**
   * 获取缓存的数量，这里会触发清空判断
   */
  @Override
  public int getSize() {
    // 判断是否要全部清空
    clearWhenStale();
    return delegate.getSize();
  }

  /**
   * 添加缓存
   */
  @Override
  public void putObject(Object key, Object object) {
    // 判断是否要全部清空
    clearWhenStale();
    delegate.putObject(key, object);
  }

  /**
   * 获取缓存
   */
  @Override
  public Object getObject(Object key) {
    // 判断是否要全部清空
    return clearWhenStale() ? null : delegate.getObject(key);
  }

  /**
   * 清除指定缓存
   */
  @Override
  public Object removeObject(Object key) {
    // 判断是否要全部清空
    clearWhenStale();
    return delegate.removeObject(key);
  }

  /**
   * 清空缓存
   */
  @Override
  public void clear() {
    // 记录清空时间
    lastClear = System.currentTimeMillis();
    // 全部清空
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
   * 获取hashcode
   */
  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  /**
   * 使用委托对象的实现
   */
  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  /**
   * 判断是否要全部清空
   *
   * @return 是否全部清空
   */
  private boolean clearWhenStale() {
    // 判断是否要全部清空
    if (System.currentTimeMillis() - lastClear > clearInterval) {
      // 清空
      clear();
      return true;
    }
    return false;
  }

}
