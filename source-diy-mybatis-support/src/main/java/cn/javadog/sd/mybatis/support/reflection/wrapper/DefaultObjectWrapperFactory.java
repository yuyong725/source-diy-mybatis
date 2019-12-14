package cn.javadog.sd.mybatis.support.reflection.wrapper;

import cn.javadog.sd.mybatis.support.exceptions.ReflectionException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;

/**
 * @author 余勇
 * @date 2019-12-02 20:02
 * 默认 ObjectWrapperFactory 实现类
 */
public class DefaultObjectWrapperFactory implements ObjectWrapperFactory {

  /**
   * 默认没有，mybatis没有对该类做实现，或者说没有将相关的包装对象缓存起来
   * TODO 在Spring中是否有实现？
   */
  @Override
  public boolean hasWrapperFor(Object object) {
    return false;
  }

  /**
   * 默认实现直接报错
   */
  @Override
  public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
    throw new ReflectionException("The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");
  }

}
