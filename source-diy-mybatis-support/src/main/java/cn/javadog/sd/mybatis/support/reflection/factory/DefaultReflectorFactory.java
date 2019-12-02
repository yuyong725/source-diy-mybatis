package cn.javadog.sd.mybatis.support.reflection.factory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.javadog.sd.mybatis.support.reflection.Reflector;
import cn.javadog.sd.mybatis.support.reflection.factory.ReflectorFactory;

/**
 * @author: 余勇
 * @date: 2019-12-01 18:05
 *
 * ReflectorFactory的默认实现
 */
public class DefaultReflectorFactory implements ReflectorFactory {
  /**
   * 是否缓存
   */
  private boolean classCacheEnabled = true;

  /**
   * Reflector 的缓存映射
   *
   * KEY：类
   * VALUE：Reflector 对象
   */
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();

  public DefaultReflectorFactory() {
  }

  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  @Override
  public Reflector findForClass(Class<?> type) {
    // 开启缓存，则从 reflectorMap 中获取
    if (classCacheEnabled) {
            // synchronized (type) removed see issue #461
      // 不存在，则进行创建
      return reflectorMap.computeIfAbsent(type, Reflector::new);
    // 关闭缓存，则创建 Reflector 对象
    } else {
      return new Reflector(type);
    }
  }

}
