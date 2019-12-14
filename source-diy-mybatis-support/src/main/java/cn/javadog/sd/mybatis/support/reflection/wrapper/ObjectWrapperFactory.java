package cn.javadog.sd.mybatis.support.reflection.wrapper;

import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;

/**
 * @author 余勇
 * @date 2019-12-02 20:01
 * ObjectWrapper 工厂接口
 */
public interface ObjectWrapperFactory {

  /**
   * 某个对象是否有包装对象
   *
   * @param object 指定对象
   * @return 是否
   */
  boolean hasWrapperFor(Object object);

  /**
   * 获得指定对象的 ObjectWrapper 对象
   *
   * @param metaObject MetaObject 对象
   * @param object 指定对象
   * @return ObjectWrapper 对象
   */
  ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);

}
