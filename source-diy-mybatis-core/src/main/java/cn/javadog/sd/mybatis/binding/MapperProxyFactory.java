package cn.javadog.sd.mybatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.javadog.sd.mybatis.session.SqlSession;

/**
 * @author: 余勇
 * @date: 2019-12-10 20:46
 * Mapper Proxy 工厂类
 */
public class MapperProxyFactory<T> {

  /**
   * Mapper 接口
   */
  private final Class<T> mapperInterface;

  /**
   * 方法与 MapperMethod 的映射
   * 值的添加通过 生成的代理对象完成，即{@link MapperProxy#cachedMapperMethod(Method)}
   */
  private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<>();

  /**
   * 构造函数
   */
  public MapperProxyFactory(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * 获取Mapper 接口
   */
  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  /**
   * 获取 与 MapperMethod 的映射
   */
  public Map<Method, MapperMethod> getMethodCache() {
    return methodCache;
  }

  /**
   * 创建mapperProxy实例的代理对象
   * note mapperProxy没有实现mapperInterface，且mapperInterface也是有很多，只需知道生成的代理对象是mapperInterface的子类即可
   *
   * @return T mapperInterface的代理对象
   */
  @SuppressWarnings("unchecked")
  protected T newInstance(MapperProxy<T> mapperProxy) {
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }

  /**
   * 创建mapperProxy的代理对象
   * 动态代理 https://www.jianshu.com/p/4df6e4d7eb46
   */
  public T newInstance(SqlSession sqlSession) {
    // 创建mapperProxy实例
    final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
    // 创建mapperProxy实例的代理对象
    return newInstance(mapperProxy);
  }

}
