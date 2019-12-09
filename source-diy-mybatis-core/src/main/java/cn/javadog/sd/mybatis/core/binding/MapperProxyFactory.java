package cn.javadog.sd.mybatis.support.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.javadog.sd.mybatis.support.base.session.SqlSession;

/**
 * @author: 余勇
 * @date: 2019-12-09 16:07
 *
 * Mapper Proxy 工厂类
 */
public class MapperProxyFactory<T> {

  /**
   * Mapper 接口
   */
  private final Class<T> mapperInterface;

  /**
   * 方法与 MapperMethod 的映射
   */
  private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<>();

  public MapperProxyFactory(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  public Map<Method, MapperMethod> getMethodCache() {
    return methodCache;
  }

  @SuppressWarnings("unchecked")
  protected T newInstance(MapperProxy<T> mapperProxy) {
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }

  /**
   * 动态代理 https://www.jianshu.com/p/4df6e4d7eb46
   */
  public T newInstance(SqlSession sqlSession) {
    final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
    return newInstance(mapperProxy);
  }

}
