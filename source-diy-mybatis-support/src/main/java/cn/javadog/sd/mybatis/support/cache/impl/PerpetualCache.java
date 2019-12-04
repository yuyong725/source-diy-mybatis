package cn.javadog.sd.mybatis.support.cache.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.exceptions.CacheException;

/**
 * @author: 余勇
 * @date: 2019-12-04 15:23
 * 永不过期的 Cache 实现类，基于 HashMap 实现类
 * 这是源码中唯一的Cache的SPI实现，二级缓存默认用的就是它
 */
public class PerpetualCache implements Cache {

  /**
   * 标识
   */
  private final String id;

  /**
   * 缓存容器
   */
  private Map<Object, Object> cache = new HashMap<>();

  /**
   * 构造，ID要求是namespace，这是规范
   */
  public PerpetualCache(String id) {
    this.id = id;
  }

  /**
   * 获取缓存标识
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * 获取容器中缓存的数量
   */
  @Override
  public int getSize() {
    return cache.size();
  }

  /**
   * 添加缓存
   */
  @Override
  public void putObject(Object key, Object value) {
    cache.put(key, value);
  }

  /**
   * 查询缓存
   */
  @Override
  public Object getObject(Object key) {
    return cache.get(key);
  }

  /**
   * 移除指定缓存
   */
  @Override
  public Object removeObject(Object key) {
    return cache.remove(key);
  }

  /**
   * 清空缓存
   */
  @Override
  public void clear() {
    cache.clear();
  }

  /**
   * 获取读写锁，这里没有做实现
   */
  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * 重写equal，判断两个cache是否指向同一个namespace
   */
  @Override
  public boolean equals(Object o) {
    if (getId() == null) {
      throw new CacheException("Cache instances require an ID.");
    }
    if (this == o) {
      return true;
    }
    if (!(o instanceof Cache)) {
      return false;
    }

    Cache otherCache = (Cache) o;
    return getId().equals(otherCache.getId());
  }

  /**
   * 重写hashcode，拿的就是标识符的code
   */
  @Override
  public int hashCode() {
    if (getId() == null) {
      throw new CacheException("Cache instances require an ID.");
    }
    return getId().hashCode();
  }

}
