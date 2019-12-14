package cn.javadog.sd.mybatis.builder;

import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author 余勇
 * @date 2019-12-12 13:15
 *
 * 对象初始化接口。实现该接口的对象，初始化完成后，会调用下面的 initialize 方法。
 * note  initialize方法的调用不是由框架完成的，只是一种约定，需要自己手动的去调用。没多大用处
 */
public interface InitializingObject {

  /**
   * 初始化对象。
   * 方法会在设置完全部属性后调用。如 {@link cn.javadog.sd.mybatis.mapping.CacheBuilder#setCacheProperties(Cache)}
   */
  void initialize() throws Exception;

}