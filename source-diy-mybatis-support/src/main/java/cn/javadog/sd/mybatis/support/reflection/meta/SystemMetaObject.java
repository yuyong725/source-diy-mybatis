package cn.javadog.sd.mybatis.support.reflection.meta;

import cn.javadog.sd.mybatis.support.reflection.factory.DefaultObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.DefaultReflectorFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.wrapper.DefaultObjectWrapperFactory;
import cn.javadog.sd.mybatis.support.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author 余勇
 * @date 2019-12-01 17:45
 *
 * 系统级的 MetaObject 对象，
 * 主要提供了 ObjectFactory、ObjectWrapperFactory、空 MetaObject 的单例。
 */
public final class SystemMetaObject {

  /**
   * ObjectFactory 的单例
   */
  public static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();

  /**
   * ObjectWrapperFactory 的单例
   */
  public static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();

  /**
   * 空对象的 MetaObject 对象单例
   */
  public static final MetaObject NULL_META_OBJECT = MetaObject.forObject(NullObject.class, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());

  /**
   * 关掉默认构造
   */
  private SystemMetaObject() {
  }

  /**
   * 内部类，适用于值为null的对象
   */
  private static class NullObject {
  }

  public static MetaObject forObject(Object object) {
    return MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
  }

}
