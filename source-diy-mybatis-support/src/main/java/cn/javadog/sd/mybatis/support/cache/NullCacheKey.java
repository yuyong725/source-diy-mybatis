package cn.javadog.sd.mybatis.support.cache;

import cn.javadog.sd.mybatis.support.exceptions.CacheException;

/**
 * @author: 余勇
 * @date: 2019-12-04 21:52
 * 空缓存键
 */
public final class NullCacheKey extends CacheKey {

  private static final long serialVersionUID = 3704229911977019465L;

  /**
   * 构造
   */
  public NullCacheKey() {
    super();
  }

  /**
   * 空键不允许更改
   */
  @Override
  public void update(Object object) {
    throw new CacheException("Not allowed to update a NullCacheKey instance.");
  }

  /**
   * 空键不允许更改
   */
  @Override
  public void updateAll(Object[] objects) {
    throw new CacheException("Not allowed to update a NullCacheKey instance.");
  }
}
