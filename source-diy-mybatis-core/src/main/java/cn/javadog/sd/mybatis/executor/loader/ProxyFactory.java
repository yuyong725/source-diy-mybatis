package cn.javadog.sd.mybatis.executor.loader;

import java.util.List;
import java.util.Properties;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;

/**
 * @author 余勇
 * @date 2019-12-15 15:27
 *
 * 代理工厂接口，用于创建需要延迟加载属性的结果对象
 */
public interface ProxyFactory {

  /**
   * 设置属性，目前是空实现。可以暂时无视该方法
   */
  void setProperties(Properties properties);

  /**
   * 创建代理对象
   */
  Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);
  
}
