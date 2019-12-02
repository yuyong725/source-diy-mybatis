package cn.javadog.sd.mybatis.support.reflection.factory;

import cn.javadog.sd.mybatis.support.reflection.Reflector;

/**
 * @author: 余勇
 * @date: 2019-12-01 17:47
 *
 * 反射类Reflector的工厂
 */
public interface ReflectorFactory {

  /**
   * @return 是否缓存 Reflector 对象
   */
  boolean isClassCacheEnabled();

  /**
   * 设置是否缓存 Reflector 对象
   *
   * @param classCacheEnabled 是否缓存
   */
  void setClassCacheEnabled(boolean classCacheEnabled);

  /**
   * 获取 Reflector 对象
   *
   * @param type 指定类
   * @return Reflector 对象
   */
  Reflector findForClass(Class<?> type);
}