package cn.javadog.sd.mybatis.support.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import cn.javadog.sd.mybatis.support.base.session.SqlSession;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * @author: 余勇
 * @date: 2019-12-09 16:07
 *
 * 实现 InvocationHandler、Serializable 接口，Mapper Proxy
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;

  /**
   * SqlSession 对象
   */
  private final SqlSession sqlSession;

  /**
   * Mapper 接口
   */
  private final Class<T> mapperInterface;

  /**
   * 方法与 MapperMethod 的映射
   *
   * 从 {@link MapperProxyFactory#methodCache} 传递过来
   */
  private final Map<Method, MapperMethod> methodCache;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // <1> 如果是 Object 定义的方法，直接调用
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
        // 见 https://github.com/mybatis/mybatis-3/issues/709 ，支持 JDK8 default 方法
      } else if (isDefaultMethod(method)) {
        return invokeDefaultMethod(proxy, method, args);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
    // <3.1> 获得 MapperMethod 对象
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    // <3.2> 执行 MapperMethod 方法
    return mapperMethod.execute(sqlSession, args);
  }

  private MapperMethod cachedMapperMethod(Method method) {
    return methodCache.computeIfAbsent(method, k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
  }

  /**
   * 进行反射调用
   */
  private Object invokeDefaultMethod(Object proxy, Method method, Object[] args)
      throws Throwable {
    final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
        .getDeclaredConstructor(Class.class, int.class);
    if (!constructor.isAccessible()) {
      constructor.setAccessible(true);
    }
    final Class<?> declaringClass = method.getDeclaringClass();
    return constructor
        .newInstance(declaringClass,
            MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
                | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC)
        .unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
  }

  /**
   * Backport of java.lang.reflect.Method#isDefault()
   */
  private boolean isDefaultMethod(Method method) {
    return (method.getModifiers()
        & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
        && method.getDeclaringClass().isInterface();
  }
}
