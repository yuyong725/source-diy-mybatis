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
   * 是否缓存 Reflector 对象，默认缓存
   */
  private boolean classCacheEnabled = true;

  /**
   * Reflector 的缓存映射
   *
   * KEY：类
   * VALUE：Reflector 对象
   */
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();

  /**
   * 默认构造，注意不是private的
   */
  public DefaultReflectorFactory() {
  }

  /**
   * 查询是否开启了缓存
   */
  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  /**
   * 设置是否缓存 Reflector 对象
   */
  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  /**
   * 获取 Reflector 对象
   *
   * @param type 指定类
   * @return Reflector 对象
   */
  @Override
  public Reflector findForClass(Class<?> type) {
    if (classCacheEnabled) {
      // 开启缓存，则从 reflectorMap 中获取，没找到就创建 => 放进去 => 缓存起来
      // note 注释里提到之前添加了 synchronized(type) ，但是移除了，原因参见see issue #461，这种我是不懂的
      return reflectorMap.computeIfAbsent(type, Reflector::new);
    } else {
      // 关闭缓存，则创建 Reflector 对象
      return new Reflector(type);
    }
  }

}
